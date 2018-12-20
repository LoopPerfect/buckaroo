namespace Buckaroo

open FSharp.Control
open Buckaroo.PackageLocation
open Buckaroo.Constraint


type LocationFragment =
| GitFragment of Revision * Set<Version>
| VersionFragment of Version * HttpPackageSource

type DefaultSourceExplorer (downloadManager : DownloadManager, gitManager : GitManager) =
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
        yield (rev, GitVersion.Tag x.Name)
      })

      yield! branches |> Seq.collect (fun x -> seq {
        let rev = x.Revision
        yield (rev, GitVersion.Branch x.Name)
      })
    }

    let all =
      allRefs
        |> Seq.groupBy (fun (r, _) -> r)
        |> Seq.map(fun (revision, aliases) ->
          (revision, aliases
            |> Seq.map (fun (r, x) -> Version.Git x)
            |> Set
            |> Set.add (Version.Git (GitVersion.Revision revision))
        ))

    System.Console.WriteLine ("git-version-fetcher: " + url + "\n" + "discovered following advertised versions:\n")
    for (_, versions) in all do
      System.Console.WriteLine (
        versions
        |> Set.toSeq
        |> Seq.map Version.show
        |> String.concat ", "
        |> (fun x -> "VersionGroup { " + x + "}")
      )

    yield! all |> AsyncSeq.ofSeq

    //let mutable revisionMap = Map.ofSeq all

    // for branch in branches do
    //   let b = GitVersion.Branch branch.Name
    //   let! commits = gitManager.FetchCommits url branch.Name
    //   for commit in commits |> Seq.tail do

    //     match revisionMap.ContainsKey commit with
    //     | false ->
    //       revisionMap <- revisionMap
    //         |> Map.add commit (Set[Version.Git (GitVersion.Revision commit)])
    //     | true -> ()

    //     let versions = revisionMap.Item commit
    //     let newVersions = versions |> Set.add (Version.Git b)
    //     revisionMap <- revisionMap
    //       |> Map.add commit newVersions

    //     yield (commit, revisionMap.Item commit)
  }




  interface ISourceExplorer with

    member this.FetchLocation versionedSource =
      match versionedSource with
      | VersionedSource.Git (g, vs) -> async { return (g, vs) }
      | VersionedSource.Http (h, vs) -> async {
          let! path = downloadManager.DownloadToCache h.Url
          let! hash = Files.sha256 path
          return (PackageLocation.Http {
            Url = h.Url
            StripPrefix = h.StripPrefix
            Type = h.Type
            Sha256 = hash
          }, vs)
        }

    member this.FetchVersions locations package =
      match package with
        | PackageIdentifier.BitBucket bb ->
          let url = PackageLocation.bitBucketUrl bb
          fetchVersionsFromGit url
            |> AsyncSeq.map (fun (rev, vs) ->
            VersionedSource.Git
              (PackageLocation.BitBucket{
                Revision = rev
                Hint = Hint.Default
                Package = bb
              }, vs))
        | PackageIdentifier.GitHub gh ->
          let url = PackageLocation.gitHubUrl gh
          fetchVersionsFromGit url
            |> AsyncSeq.map (fun (rev, vs) ->
            VersionedSource.Git
              (PackageLocation.GitHub{
                Revision = rev
                Hint = Hint.Default
                Package = gh
              }, vs))
        | PackageIdentifier.GitLab gl ->
          let url = PackageLocation.gitLabUrl gl
          fetchVersionsFromGit url
            |> AsyncSeq.map (fun (rev, vs) ->
            VersionedSource.Git
              (PackageLocation.GitLab{
                Revision = rev
                Hint = Hint.Default
                Package = gl
              }, vs))
        | PackageIdentifier.Adhoc adhoc ->
          let (_, source) =
            locations
            |> Map.toSeq
            |> Seq.find (fun (p, _) -> p = adhoc)

          match source with
          | PackageSource.Git g ->
            fetchVersionsFromGit g.Uri
            |> AsyncSeq.map (fun (rev, vs) ->
            VersionedSource.Git
              (PackageLocation.Git{
                Revision = rev
                Hint = Hint.Default
                Url = g.Uri
              }, vs))
          | PackageSource.Http h -> asyncSeq {
            yield! h
            |> Map.toSeq
            |> Seq.map (fun (version, source) ->
               VersionedSource.Http (source, Set[version]))
            |> AsyncSeq.ofSeq
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
