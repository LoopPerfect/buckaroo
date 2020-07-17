namespace Buckaroo

open FSharp.Control
open FSharpx
open Buckaroo.Console
open Buckaroo.RichOutput
open Buckaroo.Logger

type DefaultSourceExplorer (console : ConsoleManager, downloadManager : DownloadManager, gitManager : GitManager) =
  let logger = createLogger console (Some "explorer")
  let toOptional = Async.Catch >> (Async.map Choice.toOption)

  let fromFileCache url revision path =
    gitManager.GetFile url revision path |> toOptional


  // We fetch from cache or api if possible.
  // However fetching from api might be not feasable if credentials are required.
  // In such case we fallback to git and try to find the requested commit.
  let cacheOrApi (api, url : string, rev : string, path : string) = async {
    let! cached = fromFileCache url rev path
    match cached with
    | Some data -> return data
    | None ->
      match! (api rev path |> Async.Catch) with
      | Choice1Of2 result ->
        return result
      | Choice2Of2 error ->
        logger.Trace("failed to fetch file using api, falling back to git")
        do! gitManager.FindCommit url rev None
        return! fromFileCache url rev path |> Async.map (Option.getOrRaise <| error)
  }

  let extractFileFromHttp (source : HttpLocation) (filePath : string) = async {
    if Option.defaultValue ArchiveType.Zip source.Type <> ArchiveType.Zip
    then
      return raise (System.Exception "Only zip is currently supported")

    let! pathToZip = downloadManager.DownloadToCache source.Url
    use file = System.IO.File.OpenRead pathToZip
    use zip = new System.IO.Compression.ZipArchive(file, System.IO.Compression.ZipArchiveMode.Read)

    let! root = async {
      match source.StripPrefix with
      | Some stripPrefix ->
        let roots =
          zip.Entries
          |> Seq.map (fun entry -> System.IO.Path.GetDirectoryName entry.FullName)
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
            raise (System.Exception("Strip prefix " + stripPrefix + " did not match any paths! "))
        | _ ->
          return
            raise (System.Exception("Strip prefix " + stripPrefix + " matched multiple paths: " + (string roots)))
      | None ->
        return ""
    }

    use stream = zip.GetEntry(System.IO.Path.Combine(root, filePath)).Open()
    use streamReader = new System.IO.StreamReader(stream)

    return!
      streamReader.ReadToEndAsync ()
      |> Async.AwaitTask
  }

  let fetchFile location path =
    match location with
    | PackageLock.BitBucket bitBucket ->
      let url = PackageLocation.gitHubUrl bitBucket.Package
      cacheOrApi (BitBucketApi.fetchFile bitBucket.Package, url, bitBucket.Revision, path)
    | PackageLock.GitHub gitHub ->
      let url = PackageLocation.gitHubUrl gitHub.Package
      cacheOrApi (GitHubApi.fetchFile gitHub.Package, url, gitHub.Revision, path)
    | PackageLock.GitLab gitLab ->
      let url = PackageLocation.gitLabUrl gitLab.Package
      cacheOrApi (GitLabApi.fetchFile gitLab.Package, url, gitLab.Revision, path)
    | PackageLock.Git git ->
      let url = git.Url
      cacheOrApi(gitManager.GetFile git.Url, url, git.Revision, path)
    | PackageLock.Http (http, _) ->
      extractFileFromHttp http path

  let branchPriority branch =
    match branch with
    | "master" -> 0
    | "develop" -> 1
    | _ -> 2

  let fetchRevisionsFromGitTag url tag = asyncSeq {
    let! refs = gitManager.FetchRefs url

    yield!
      refs
      |> Seq.filter (fun ref -> ref.Type = RefType.Tag && ref.Name = tag)
      |> Seq.map (fun ref -> ref.Revision)
      |> AsyncSeq.ofSeq
  }

  let fetchRevisionsFromGitBranch url branch = asyncSeq {
    let! refs = gitManager.FetchRefs url
    let maybeBranchRef =
      refs
      |> Seq.tryFind (fun ref -> ref.Type = RefType.Branch && ref.Name = branch)

    match maybeBranchRef with
    | Some branchRef ->
      yield branchRef.Revision
      yield! gitManager.FetchCommits url branchRef.Name
      ()
    | None -> ()
  }

  let fetchRevisionsFromGitSemVer url semVer = asyncSeq {
    let! refs = gitManager.FetchRefs url

    yield!
      refs
      |> Seq.choose (fun ref ->
        if ref.Type = RefType.Tag
        then
          match SemVer.parse ref.Name with
          | Result.Ok parsedSemVer ->
            if parsedSemVer = semVer
            then
              Some ref.Revision
            else
              None
          | _ -> None
        else
          None
      )
      |> AsyncSeq.ofSeq
  }

  let fetchRevisionsFromGitVersion (gitUrl : string) (version : Version) = asyncSeq {
    match version with
    | Version.Git (GitVersion.Branch branch) ->
      yield! fetchRevisionsFromGitBranch gitUrl branch
    | Version.Git (GitVersion.Tag tag) ->
      yield! fetchRevisionsFromGitTag gitUrl tag
    | Version.Git (GitVersion.Revision revision) ->
      yield revision
    | Version.SemVer semVer ->
      yield! fetchRevisionsFromGitSemVer gitUrl semVer
  }

  let fetchVersionsFromGit gitUrl = asyncSeq {
    let! refs = gitManager.FetchRefs gitUrl

    // Sem-vers
    yield!
      refs
      |> Seq.choose (fun ref ->
        match (ref.Type, SemVer.parse ref.Name) with
        | (RefType.Tag, Result.Ok semVer) -> Some (Version.SemVer semVer)
        | _ -> None
      )
      |> Seq.sortDescending
      |> AsyncSeq.ofSeq

    // Tags
    yield!
      refs
      |> Seq.choose (fun ref ->
        match ref.Type with
        | RefType.Tag -> Some (Version.Git (GitVersion.Tag ref.Name))
        | _ -> None
      )
      |> AsyncSeq.ofSeq

    // Default branch
    let! gitPath = gitManager.Clone gitUrl
    let! defaultBranch = gitManager.DefaultBranch gitPath
    yield Version.Git (GitVersion.Branch defaultBranch)

    // Branches
    yield!
      refs
      |> Seq.sortBy (fun x -> branchPriority x.Name)
      |> Seq.choose (fun ref ->
        match ref.Type with
        | RefType.Branch ->
          if ref.Name <> defaultBranch
          then
            Some (Version.Git (GitVersion.Branch ref.Name))
          else
            None
        | _ -> None
      )
      |> AsyncSeq.ofSeq

    // TODO: Revisions?
  }

  interface ISourceExplorer with

    member this.FetchVersions locations package = asyncSeq {
      match package with
      | PackageIdentifier.GitHub gitHub ->
        yield! fetchVersionsFromGit (PackageLocation.gitHubUrl gitHub)
      | PackageIdentifier.GitLab gitLab ->
        yield! fetchVersionsFromGit (PackageLocation.gitLabUrl gitLab)
      | PackageIdentifier.BitBucket bitBucket ->
        yield! fetchVersionsFromGit (PackageLocation.bitBucketUrl bitBucket)
      | PackageIdentifier.Adhoc adhoc ->
        match locations |> Map.tryFind adhoc with
        | Some (PackageSource.Git git) ->
          yield! fetchVersionsFromGit git.Uri
        | Some (PackageSource.Http versions) ->
          yield!
            versions
            |> Map.toSeq
            |> Seq.map fst
            |> Seq.distinct
            |> AsyncSeq.ofSeq
        | _ -> ()
    }

    member this.LockLocation packageLocation = async {
      // TODO: Verify that Git commit actually exists
      match packageLocation with
      | PackageLocation.GitHub gitHub ->
        return PackageLock.GitHub gitHub
      | PackageLocation.BitBucket bitBucket ->
        return PackageLock.BitBucket bitBucket
      | PackageLocation.GitLab gitLab ->
        return PackageLock.GitLab gitLab
      | PackageLocation.Git g ->
        return PackageLock.Git g
      | PackageLocation.Http h ->
        let! path = downloadManager.DownloadToCache h.Url
        let! hash = Files.sha256 path

        return
          PackageLock.Http
            (
              {
                Url = h.Url;
                StripPrefix = h.StripPrefix;
                Type = h.Type;
              },
              hash
            )
    }

    member this.FetchLocations locations package version = asyncSeq {
      match package with
      | PackageIdentifier.GitHub gitHub ->
        let gitUrl = PackageLocation.gitHubUrl gitHub
        yield!
          fetchRevisionsFromGitVersion gitUrl version
          |> AsyncSeq.map (fun revision ->
            PackageLocation.GitHub {
              Package = gitHub;
              Revision = revision;
            }
          )
      | PackageIdentifier.GitLab gitLab ->
        let gitUrl = PackageLocation.gitLabUrl gitLab
        yield!
          fetchRevisionsFromGitVersion gitUrl version
          |> AsyncSeq.map (fun revision ->
            PackageLocation.GitLab {
              Package = gitLab;
              Revision = revision;
            }
          )
      | PackageIdentifier.BitBucket bitBucket ->
        let gitUrl = PackageLocation.bitBucketUrl bitBucket
        yield!
          fetchRevisionsFromGitVersion gitUrl version
          |> AsyncSeq.map (fun revision ->
            PackageLocation.BitBucket {
              Package = bitBucket;
              Revision = revision;
            }
          )
      | PackageIdentifier.Adhoc adhoc ->
        match locations |> Map.tryFind adhoc with
        | Some (PackageSource.Git git) ->
          yield!
            fetchRevisionsFromGitVersion git.Uri version
            |> AsyncSeq.map (fun revision ->
              PackageLocation.Git {
                Url = git.Uri;
                Revision = revision;
              }
            )
        | Some (PackageSource.Http versions) ->
          match versions |> Map.tryFind version with
          | Some location -> yield PackageLocation.Http location
          | None -> ()
        | _ -> ()
    }

    member this.FetchManifest (location, versions) =
      async {
        let! maybeContent =
          fetchFile location Constants.ManifestFileName
          |> toOptional

        match maybeContent with
        | Some content ->
          return
            match Manifest.parse content with
            | Result.Ok manifest -> manifest
            | Result.Error error ->
              let errorMessage =
                "Invalid " + Constants.ManifestFileName + " file. \n" +
                (Manifest.ManifestParseError.show error)
              raise <| System.Exception errorMessage
        | None ->
          return raise <| System.Exception ("No manifest was found at " + (PackageLock.show location) + ". ")
      }

    member this.FetchLock (location, versions) =
      async {
        let! maybeContent =
          fetchFile location Constants.LockFileName
          |> Async.Catch
          |> Async.map Choice.toOption

        return
          match maybeContent with
          | None ->
            logger.RichWarning (
              (text "Could not fetch ") + (highlight Constants.LockFileName) + (text " from ") +
              (PackageLock.show location |> highlight) + (warn " 404"))
            raise <| System.Exception ("Could not fetch " + Constants.LockFileName + " file")
          | Some content ->
            match Lock.parse content with
            | Result.Ok manifest -> manifest
            | Result.Error errorMessage ->
              logger.RichWarning(
                (text "Could not parse ") + (highlight Constants.LockFileName) + (text " from ") +
                (PackageLock.show location |> highlight) + (text " ") + (warn errorMessage))
              System.Exception("Invalid " + Constants.LockFileName + " file") |> raise
      }
