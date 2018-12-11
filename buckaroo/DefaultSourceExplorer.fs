namespace Buckaroo

open FSharp.Control
open Buckaroo.PackageLocation

type DefaultSourceExplorer (downloadManager : DownloadManager, gitManager : GitManager) = 

  let branchPriority branch = 
    match branch with 
    | "master" -> 0
    | "develop" -> 1
    | _ -> 2

  let fetchLocationsFromGit (url : string) (version : Version) = asyncSeq {
    match version with 
    | Buckaroo.SemVerVersion semVer -> 
      let! tags = gitManager.FetchTags url
      yield! 
        tags 
        |> Seq.filter (fun t -> SemVer.parse t.Name = Result.Ok semVer)
        |> Seq.map (fun t -> t.Commit)
        |> Seq.distinct
        |> Seq.map (fun x -> (Hint.Default, x))
        |> AsyncSeq.ofSeq
    | Buckaroo.Version.Branch branch -> 
      let! branches = gitManager.FetchBranches url

      yield! 
        branches
        |> Seq.filter (fun x -> x.Name = branch)
        |> Seq.map (fun x -> (Hint.Branch x.Name, x.Head))
        |> AsyncSeq.ofSeq

      do! gitManager.FetchBranch url branch
      let! commits = gitManager.FetchCommits url branch
      yield!
        commits
        |> Seq.except (branches |> Seq.map (fun x -> x.Head))
        |> Seq.map (fun x -> (Hint.Branch branch, x))
        |> AsyncSeq.ofSeq

    | Buckaroo.Version.Revision r -> 
      yield (Hint.Default, r)
    | Buckaroo.Version.Tag tag -> 
      let! tags = gitManager.FetchTags url
      yield! 
        tags 
        |> Seq.filter (fun t -> t.Name = tag)
        |> Seq.map (fun t -> t.Commit)
        |> Seq.distinct
        |> Seq.map (fun x -> (Hint.Default, x))
        |> AsyncSeq.ofSeq
    | Buckaroo.Version.Latest -> ()
  }

  let fetchLocationsFromPackageSource (source : PackageSource) (version : Version) = asyncSeq {
    match source with 
    | PackageSource.Http http -> 
      let! path = downloadManager.DownloadToCache http.Url
      let! hash = Files.sha256 path
      yield 
        PackageLocation.Http
        {
          Url = http.Url; 
          StripPrefix = http.StripPrefix; 
          Type = http.Type;
          Sha256 = hash; 
        }
  }

  let extractFileFromHttp (source : HttpLocation) (filePath : string) = async {
    if Option.defaultValue ArchiveType.Zip source.Type <> ArchiveType.Zip 
    then
      return raise (new System.Exception("Only zip is currently supported"))
    
    let! pathToZip = downloadManager.DownloadToCache source.Url
    use file = System.IO.File.OpenRead pathToZip
    use zip = new System.IO.Compression.ZipArchive(file, System.IO.Compression.ZipArchiveMode.Read)

    let! root = async {
      match source.StripPrefix with 
      | Some stripPrefix -> 
        let roots = 
          zip.Entries
          |> Seq.map (fun entry -> System.IO.Path.GetDirectoryName(entry.FullName))
          |> Seq.distinct
          |> Seq.filter (fun directory -> 
            directory |> Glob.isLike stripPrefix
          )
          |> Seq.truncate 2
          |> Seq.toList
        match roots with 
        | [ root ] -> return root
        | [] -> 
          return 
            raise (new System.Exception("Strip prefix " + stripPrefix + " did not match any paths! "))
        | _ -> 
          return 
            raise (new System.Exception("Strip prefix " + stripPrefix + " matched multiple paths: " + (string roots)))
      | None -> 
        return ""
    }

    use stream = zip.GetEntry(System.IO.Path.Combine(root, filePath)).Open()
    use streamReader = new System.IO.StreamReader(stream)

    return! 
      streamReader.ReadToEndAsync() |> Async.AwaitTask
  }

  let fetchVersionsFromGit (url : string) = asyncSeq {
    let! branchesTask = 
      gitManager.FetchBranches url
      |> Async.StartChild

    // Tags and sem-vers
    let! tags = gitManager.FetchTags url

    yield! 
      tags 
      |> Seq.collect (fun tag -> seq {
        match SemVer.parse tag.Name with
        | Result.Ok semVer -> 
          yield semVer |> Version.SemVerVersion
          ()
        | Result.Error _ -> 
          ()
        yield tag.Name |> Version.Tag
      })
      |> Seq.sortWith (fun x y -> 
        match (x, y) with 
        | (SemVerVersion i, SemVerVersion j) -> SemVer.compare i j
        | (SemVerVersion _, Version.Tag _) -> -1
        | (Version.Tag _, SemVerVersion _) -> 1
        | (Version.Tag i, Version.Tag j) -> System.String.Compare(i, j)
        | _ -> 0
      )
      |> AsyncSeq.ofSeq

    let! branches = branchesTask

    // Branches
    yield! 
      branches 
      |> Seq.map (fun x -> x.Name)
      |> Seq.sortBy branchPriority
      |> Seq.map Version.Branch
      |> AsyncSeq.ofSeq

    // Tag Revisions
    yield!
      tags
      |> Seq.map (fun x -> Buckaroo.Version.Revision x.Commit)
      |> AsyncSeq.ofSeq

    // Branch Revisions
    yield!
      branches
      |> Seq.map (fun x -> Buckaroo.Version.Revision x.Head)
      |> AsyncSeq.ofSeq

    let alreadyYielded = 
      branches 
      |> Seq.map (fun x -> x.Head)
      |> Seq.append (tags |> Seq.map (fun x -> x.Commit))
      |> Set.ofSeq

    // All Revisions
    for branch in branches do 
      let! commits = gitManager.FetchCommits url branch.Name
      yield! 
        commits
        |> Seq.except alreadyYielded
        |> Seq.map (fun x -> Buckaroo.Version.Revision x)
        |> AsyncSeq.ofSeq
  }

  let fetchFile location path = 
    match location with 
    | PackageLocation.GitHub gitHub -> 
      GitHubApi.fetchFile gitHub.Package gitHub.Revision path
    | PackageLocation.BitBucket bitBucket -> 
      BitBucketApi.fetchFile bitBucket.Package bitBucket.Revision path
    | PackageLocation.GitLab gitLab -> 
      GitLabApi.fetchFile gitLab.Package gitLab.Revision path
    | PackageLocation.Git git -> 
      gitManager.FetchFile git.Url git.Revision path (hintToBranch git.Hint)
    | PackageLocation.Http http -> 
      extractFileFromHttp http path

  interface ISourceExplorer with 

    member this.FetchVersions locations package = 
      match package with 
      | PackageIdentifier.GitHub gitHub -> 
        let url = PackageLocation.gitHubUrl gitHub
        fetchVersionsFromGit url
      | PackageIdentifier.BitBucket bitBucket -> 
        let url = PackageLocation.bitBucketUrl bitBucket
        fetchVersionsFromGit url
      | PackageIdentifier.GitLab gitLab -> 
        let url = PackageLocation.gitLabUrl gitLab
        fetchVersionsFromGit url
      | PackageIdentifier.Adhoc adhoc -> 
        let xs = 
          locations 
          |> Map.toSeq
          |> Seq.filter (fun ((p, _), _) -> p = adhoc)
          |> Seq.map (fst >> snd)
          |> Seq.toList
        xs
        |> AsyncSeq.ofSeq

    member this.FetchLocations locations package version = asyncSeq {
      match package with 
      | PackageIdentifier.GitHub gitHub -> 
        let url = PackageLocation.gitHubUrl gitHub
        yield! 
          fetchLocationsFromGit url version
          |> AsyncSeq.map (fun (hint, revision) -> 
            PackageLocation.GitHub {
              Package = gitHub; 
              Hint = hint; 
              Revision = revision; 
            }
          )
      | PackageIdentifier.BitBucket bitBucket -> 
        let url = PackageLocation.bitBucketUrl bitBucket
        yield! 
          fetchLocationsFromGit url version
          |> AsyncSeq.map (fun (hint, revision) -> 
            PackageLocation.BitBucket {
              Package = bitBucket; 
              Hint = hint; 
              Revision = revision; 
            }
          )
      | PackageIdentifier.GitLab gitLab -> 
        let url = PackageLocation.gitLabUrl gitLab
        yield! 
          fetchLocationsFromGit url version
          |> AsyncSeq.map (fun (hint, revision) -> 
            PackageLocation.GitLab {
              Package = gitLab; 
              Hint = hint; 
              Revision = revision; 
            }
          )
      | PackageIdentifier.Adhoc adhoc -> 
        match locations |> Map.tryFind (adhoc, version) with 
        | Some source -> 
          yield! fetchLocationsFromPackageSource source version
        | None -> 
          let message = 
            "No location specified for " + 
            (PackageIdentifier.show package) + "@" + (Version.show version)
          
          return new System.Exception(message) |> raise
    }

    member this.FetchManifest location = 
      async {
        let! content = fetchFile location Constants.ManifestFileName
        return 
          match Manifest.parse content with
          | Result.Ok manifest -> manifest
          | Result.Error error -> 
            let errorMessage = 
              "Invalid " + Constants.ManifestFileName + " file. \n" + 
              (Manifest.ManifestParseError.show error)
            new System.Exception(errorMessage)
            |> raise
      }

    member this.FetchLock location = 
      async {
        let! content = fetchFile location Constants.LockFileName
        return 
          match Lock.parse content with
          | Result.Ok manifest -> manifest
          | Result.Error errorMessage -> 
            new System.Exception("Invalid " + Constants.LockFileName + " file. \n" + errorMessage)
            |> raise
      }
