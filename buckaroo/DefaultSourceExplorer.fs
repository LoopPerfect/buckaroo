namespace Buckaroo

open FSharp.Control
open Buckaroo.PackageLock
open Buckaroo.Constraint
open Buckaroo.Console

type DefaultSourceExplorer (console : ConsoleManager, downloadManager : DownloadManager, gitManager : GitManager) =
  let extractFileFromHttp (source : HttpLocation) (sha : string) (filePath : string) = async {
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

  let fetchFile location path =
    match location with
    | PackageLock.BitBucket bitBucket ->
      BitBucketApi.fetchFile bitBucket.Package bitBucket.Revision path
    | PackageLock.GitHub gitHub ->
      GitHubApi.fetchFile gitHub.Package gitHub.Revision path
    | PackageLock.GitLab gitLab ->
      GitLabApi.fetchFile gitLab.Package gitLab.Revision path
    | PackageLock.Git git ->
      gitManager.FetchFile git.Url git.Revision path
    | PackageLock.Http (http, sha256) ->
      extractFileFromHttp http sha256 path

  let branchPriority branch =
    match branch with
    | "master" -> 0
    | "develop" -> 1
    | _ -> 2

  let fetchVersionsFromGit url = asyncSeq {
    let! refs = gitManager.FetchRefs url

    let tags = refs |> Seq.choose (fun x -> match x.Type with | RefType.Tag -> Some(x) | _ -> None)
    let branches = refs |> Seq.choose (fun x -> match x.Type with | RefType.Branch -> Some(x) | _ -> None)

    let allRefs = seq {
      yield! tags |> Seq.collect (fun x -> seq {
        let rev = x.Revision
        yield (rev, Version.Git (GitVersion.Tag x.Name))

        match SemVer.parse x.Name with
        | Result.Ok semVer ->
          yield (rev, Version.SemVer semVer)
        | _ -> ()
      })

      yield! branches |> Seq.collect (fun x -> seq {
        let rev = x.Revision
        yield (rev, Version.Git (GitVersion.Branch x.Name))
      })
    }

    let all =
      allRefs
        |> Seq.groupBy (fun (r, _) -> r)
        |> Seq.map(fun (revision, aliases) ->
          (revision, aliases
            |> Seq.map (fun (_, x) -> x)
            |> Set
            |> Set.add (Version.Git (GitVersion.Revision revision))
        ))

    console.Write("git-version-fetcher: " + url + "\n" + "discovered following advertised versions:\n", LoggingLevel.Trace)
    for (_, versions) in all do
      console.Write (
        versions
        |> Set.toSeq
        |> Seq.map Version.show
        |> String.concat ", "
        |> (fun x -> "VersionGroup {" + x + "}"),
        LoggingLevel.Trace
      )

    yield! all
      |> Seq.sortWith (fun (_, x) (_, y) -> -Version.compare x.MaximumElement y.MaximumElement)
      |> AsyncSeq.ofSeq

    console.Write("git-version-fetcher: " + url + "\n" + "exploring individual branches now", LoggingLevel.Trace)
    let mutable revisionMap = Map.ofSeq all
    for branch in branches do
      console.Write("git-version-fetcher: " + url + "\n" + "exploring branch: " + branch.Name, LoggingLevel.Trace)
      let b = GitVersion.Branch branch.Name
      let! commits = gitManager.FetchCommits url branch.Name
      for commit in commits |> Seq.tail do

        match revisionMap.ContainsKey commit with
        | false ->
          revisionMap <- revisionMap
        |> Map.add commit (Set[Version.Git (GitVersion.Revision commit)])
        | true -> ()

        let versions = revisionMap.Item commit
        let newVersions = versions |> Set.add (Version.Git b)
        revisionMap <- revisionMap
          |> Map.add commit newVersions
        yield (commit, revisionMap.Item commit)
  }

  interface ISourceExplorer with

    member this.LockLocation packageLocation = async {
      match packageLocation with
      | PackageLocation.GitHub gitHub ->
        return PackageLock.GitHub gitHub
      | PackageLocation.Git g ->
        // TODO: Verify that the commit actually exists
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

    member this.FetchLocations locations package versionConstraint =

      let fetchLocationsForVersion (version : Version) = asyncSeq {
        match package with
        | PackageIdentifier.GitHub gitHub ->
          let gitUrl = PackageLocation.gitHubUrl gitHub
          match version with
          | Version.Git (Branch branch) ->
            let! refs = gitManager.FetchRefs gitUrl
            yield!
              refs
              |> Seq.filter (fun t -> t.Name = branch)
              |> Seq.choose (fun r ->
                match r.Type with
                | RefType.Branch -> Some r.Revision
                | RefType.Tag -> None
              )
              |> Seq.map (fun r -> PackageLocation.GitHub {
                Package = gitHub;
                Revision = r;
              })
              |> AsyncSeq.ofSeq

            let! commits = gitManager.FetchCommits gitUrl branch
            yield!
              commits
              |> Seq.map (fun r -> PackageLocation.GitHub {
                Package = gitHub;
                Revision = r;
              })
              |> AsyncSeq.ofSeq
          | Version.Git (Tag tag) ->
            let! refs = gitManager.FetchRefs gitUrl
            yield!
              refs
              |> Seq.filter (fun t -> t.Name = tag)
              |> Seq.choose (fun r ->
                match r.Type with
                | RefType.Branch -> None
                | RefType.Tag -> Some r.Revision
              )
              |> Seq.map (fun r -> PackageLocation.GitHub {
                Package = gitHub;
                Revision = r;
              })
              |> AsyncSeq.ofSeq
        // TODO: All cases!
        ()
      }

      let rec loop (versionConstraint : Constraint) = asyncSeq {
        match versionConstraint with
        | Complement c ->
          let! complement =
            loop c
            |> AsyncSeq.map fst
            |> AsyncSeq.fold (fun s x -> Set.add x s) Set.empty

          yield!
            loop (Constraint.All [])
            |> AsyncSeq.filter (fun (location, _) -> complement |> Set.contains location |> not)
        | Any xs ->
          yield!
            xs
            |> List.distinct
            |> List.map loop
            |> AsyncSeq.mergeAll
            |> AsyncSeq.distinctUntilChanged
        | All xs ->
          let combine (xs : Set<PackageLocation * Set<Version>>) (ys : Set<PackageLocation * Set<Version>>) =
            xs
            |> Seq.choose (fun (location, versions) ->
              let matchingVersions =
                ys
                |> Seq.filter (fst >> (=) location)
                |> Seq.collect snd
                |> Seq.toList

              match matchingVersions with
              | [] -> None
              | vs -> Some (location, versions |> Set.union (set vs))
            )
            |> Set.ofSeq

          // TODO: add all versions stream for empty case
          yield!
            xs
            |> List.distinct
            |> List.map (loop >> (AsyncSeq.scan (fun s x -> Set.add x s) Set.empty))
            |> List.reduce (AsyncSeq.combineLatestWith combine)
            |> AsyncSeq.concatSeq
            |> AsyncSeq.distinctUntilChanged
        | Exactly v ->
          yield!
            fetchLocationsForVersion v
            |> AsyncSeq.map (fun location -> (location, Set.singleton v))
      }
      loop versionConstraint

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
