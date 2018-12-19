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
      let! refs = gitManager.FetchRefs url
      yield!
        refs
        |> Seq.choose (fun x -> match x.Type with | RefType.Tag -> Some(x) | _ -> None)
        |> Seq.filter (fun t -> SemVer.parse t.Name = Result.Ok semVer)
        |> Seq.map (fun t -> t.Revision)
        |> Seq.distinct
        |> Seq.map (fun x -> (Hint.Default, x))
        |> AsyncSeq.ofSeq
    | Buckaroo.Version.Branch branch ->
      let! refs = gitManager.FetchRefs url
      let  branches =
        refs
        |> Seq.choose (fun x -> match x.Type with | RefType.Branch -> Some(x) | _ -> None)
      yield!
        branches
        |> Seq.filter (fun x -> x.Name = branch)
        |> Seq.map (fun x -> (Hint.Branch x.Name, x.Revision))
        |> AsyncSeq.ofSeq

      do! gitManager.FetchBranch url branch
      let! commits = gitManager.FetchCommits url branch
      yield!
        commits
        |> Seq.except (branches |> Seq.map (fun x -> x.Revision))
        |> Seq.map (fun x -> (Hint.Branch branch, x))
        |> AsyncSeq.ofSeq

    | Buckaroo.Version.Revision r ->
      yield (Hint.Default, r)
    | Buckaroo.Version.Tag tag ->
      let! refs = gitManager.FetchRefs url
      yield!
        refs
        |> Seq.choose (fun x -> match x.Type with | RefType.Tag -> Some(x) | _ -> None)
        |> Seq.filter (fun t -> t.Name = tag)
        |> Seq.map (fun t -> t.Revision)
        |> Seq.distinct
        |> Seq.map (fun x -> (Hint.Default, x))
        |> AsyncSeq.ofSeq
    | Buckaroo.Version.Latest -> ()
  }

  let fetchLocationsFromPackageSource (source : PackageSource) (version : Buckaroo.Version) = asyncSeq {
    match source with
    | PackageSource.Git git ->
      yield!
        fetchLocationsFromGit git.Uri version
        |> AsyncSeq.map (fun (hint, revision) ->
          PackageLocation.Git {
            Url = git.Uri;
            Hint = hint;
            Revision = revision;
          }
        )
    | PackageSource.Http httpSources ->
      match httpSources |> Map.tryFind version with
      | None ->
        raise (new System.SystemException ("version:" + (string version) + "not found"))
      | Some http ->
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
    let! refs = gitManager.FetchRefs url

    // Tags and sem-vers
    let tags = refs |> Seq.choose (fun x -> match x.Type with | RefType.Tag -> Some(x) | _ -> None)
    let branches = refs |> Seq.choose (fun x -> match x.Type with | RefType.Branch -> Some(x) | _ -> None)

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
      |> Seq.map (fun x -> Buckaroo.Version.Revision x.Revision)
      |> AsyncSeq.ofSeq

    // Branch Revisions
    yield!
      branches
      |> Seq.map (fun x -> Buckaroo.Version.Revision x.Revision)
      |> AsyncSeq.ofSeq

    let alreadyYielded =
      branches
      |> Seq.map (fun x -> x.Revision)
      |> Seq.append (tags |> Seq.map (fun x -> x.Revision))
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
    | PackageLocation.BitBucket bitBucket ->
      BitBucketApi.fetchFile bitBucket.Package bitBucket.Revision path
    | PackageLocation.GitHub gitHub ->
      GitHubApi.fetchFile gitHub.Package gitHub.Revision path
    | PackageLocation.GitLab gitLab ->
      GitLabApi.fetchFile gitLab.Package gitLab.Revision path
    | PackageLocation.Git git ->
      gitManager.FetchFile git.Url git.Revision path (hintToBranch git.Hint)
    | PackageLocation.Http http ->
      extractFileFromHttp http path

  interface ISourceExplorer with

    member this.FetchVersions locations package =
      match package with
      | PackageIdentifier.BitBucket bitBucket ->
        let url = PackageLocation.bitBucketUrl bitBucket
        fetchVersionsFromGit url
      | PackageIdentifier.GitHub gitHub ->
        let url = PackageLocation.gitHubUrl gitHub
        fetchVersionsFromGit url
      | PackageIdentifier.GitLab gitLab ->
        let url = PackageLocation.gitLabUrl gitLab
        fetchVersionsFromGit url
      | PackageIdentifier.Adhoc adhoc ->
        let (_, source) =
          locations
          |> Map.toSeq
          |> Seq.find (fun (p, _) -> p = adhoc)

        match source with
        | PackageSource.Git g -> fetchVersionsFromGit g.Uri
        | PackageSource.Http h -> asyncSeq {
          for (v, _) in h |> Map.toSeq do
            yield v
        }

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
        match locations |> Map.tryFind adhoc with
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
