namespace Buckaroo

open System
open FSharp.Control
open Buckaroo.Git

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

  let fetchFile location path = async {
    System.Console.WriteLine("fetchFile " + (string location) + "/" + path)
    match location with 
    | PackageLocation.GitHub g -> 
      return! GitHubApi.fetchFile g.Package g.Revision path
    | PackageLocation.Git g -> 
      return! gitManager.FetchFile g.Url g.Revision path
    | PackageLocation.Http http -> 
      System.Console.WriteLine(string location)

      return (raise <| new Exception("Only Git and GitHub packages are supported"))
  }

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
            |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Revision = x })
            |> AsyncSeq.ofSeq
        | Buckaroo.Version.Branch branch -> 
          let! branches = gitManager.FetchBranches url

          yield! 
            branches
            |> Seq.filter (fun x -> x.Name = branch)
            |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Revision = x.Head })
            |> AsyncSeq.ofSeq

          do! gitManager.Fetch url branch
          let! commits = gitManager.FetchCommits url branch
          yield!
            commits
            |> Seq.except (branches |> Seq.map (fun x -> x.Head))
            |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Revision = x })
            |> AsyncSeq.ofSeq

        | Buckaroo.Version.Revision r -> 
          yield PackageLocation.GitHub { Package = g; Revision = r }
        | Buckaroo.Version.Tag tag -> 
          let! tags = gitManager.FetchTags url
          yield! 
            tags 
            |> Seq.filter (fun t -> t.Name = tag)
            |> Seq.map (fun t -> t.Commit)
            |> Seq.distinct
            |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Revision = x })
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
