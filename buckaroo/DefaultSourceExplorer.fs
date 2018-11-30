namespace Buckaroo

open System
open FSharp.Control
open Buckaroo.Git
open Buckaroo.PackageLocation

type DefaultSourceExplorer (downloadManager : DownloadManager, gitManager : GitManager) = 

  let branchPriority branch = 
    match branch with 
    | "master" -> 0
    | "develop" -> 1
    | _ -> 2

  let fetchLocationsFromPackageSource (source : PackageSource) = asyncSeq {
    match source with 
    | PackageSource.Http http -> 
      let! path = downloadManager.DonwloadToCache http.Url
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
      return raise (new Exception("Only zip is currently supported"))
    
    let! pathToZip = downloadManager.DonwloadToCache source.Url
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
            raise (new Exception("Strip prefix " + stripPrefix + " did not match any paths! "))
        | _ -> 
          return 
            raise (new Exception("Strip prefix " + stripPrefix + " matched multiple paths: " + (string roots)))
      | None -> 
        return ""
    }

    use stream = zip.GetEntry(System.IO.Path.Combine(root, filePath)).Open()
    use streamReader = new System.IO.StreamReader(stream)

    return! 
      streamReader.ReadToEndAsync() |> Async.AwaitTask
  }

  let fetchVersionsFromGit (url : String) = asyncSeq {
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
        | (Version.Tag i, Version.Tag j) -> String.Compare(i, j)
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
    | PackageLocation.Git git -> 
      gitManager.FetchFile git.Url git.Revision path (hintToBranch git.Hint)
    | PackageLocation.Http http -> 
      System.Console.WriteLine(string http)
      extractFileFromHttp http path

  interface ISourceExplorer with 

    member this.FetchVersions locations package = 
      match package with 
      | PackageIdentifier.GitHub gitHub -> 
        let url = PackageLocation.gitHubUrl gitHub
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
      | PackageIdentifier.GitHub g -> 
        let url = PackageLocation.gitHubUrl g
        match version with 
        | Buckaroo.SemVerVersion semVer -> 
          let! tags = gitManager.FetchTags url
          yield! 
            tags 
            |> Seq.filter (fun t -> SemVer.parse t.Name = Result.Ok semVer)
            |> Seq.map (fun t -> t.Commit)
            |> Seq.distinct
            |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Hint = Hint.Default; Revision = x })
            |> AsyncSeq.ofSeq
        | Buckaroo.Version.Branch branch -> 
          let! branches = gitManager.FetchBranches url

          yield! 
            branches
            |> Seq.filter (fun x -> x.Name = branch)
            |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Hint = Hint.Branch x.Name; Revision = x.Head })
            |> AsyncSeq.ofSeq

          do! gitManager.FetchBranch url branch
          let! commits = gitManager.FetchCommits url branch
          yield!
            commits
            |> Seq.except (branches |> Seq.map (fun x -> x.Head))
            |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Hint = Hint.Branch branch;  Revision = x })
            |> AsyncSeq.ofSeq

        | Buckaroo.Version.Revision r -> 
          yield PackageLocation.GitHub { Package = g; Hint = Hint.Default; Revision = r }
        | Buckaroo.Version.Tag tag -> 
          let! tags = gitManager.FetchTags url
          yield! 
            tags 
            |> Seq.filter (fun t -> t.Name = tag)
            |> Seq.map (fun t -> t.Commit)
            |> Seq.distinct
            |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Hint = Hint.Default; Revision = x })
            |> AsyncSeq.ofSeq
        | Buckaroo.Version.Latest -> ()
      | PackageIdentifier.Adhoc adhoc -> 
        match locations |> Map.tryFind (adhoc, version) with 
        | Some source -> 
          yield! fetchLocationsFromPackageSource source
        | None -> 
          return new Exception("No location specified for " + (PackageIdentifier.show package) + "@" + (Version.show version)) |> raise
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
            new Exception(errorMessage)
            |> raise
      }

    member this.FetchLock location = 
      async {
        let! content = fetchFile location Constants.LockFileName
        return 
          match Lock.parse content with
          | Result.Ok manifest -> manifest
          | Result.Error errorMessage -> 
            new Exception("Invalid " + Constants.LockFileName + " file. \n" + errorMessage)
            |> raise
      }
