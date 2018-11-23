namespace Buckaroo

open System
open FSharp.Control
open Buckaroo.Git

type DefaultSourceExplorer (gitManager : GitManager) = 

  let gitUrl (package : PackageIdentifier) : Async<string> = async {
    return 
      match package with 
      | PackageIdentifier.GitHub x -> PackageLocation.gitHubUrl x
      | _ -> 
        // TODO
        new Exception("Only GitHub projects are currently supported") |> raise
  }

  let branchPriority branch = 
    match branch with 
    | "master" -> 0
    | "develop" -> 1
    | _ -> 2

  interface ISourceExplorer with 

    member this.FetchVersions package = asyncSeq {
      let! url = gitUrl package

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

      let! branches = gitManager.FetchBranches url

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

    member this.FetchLocations package version = asyncSeq {
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
        | Buckaroo.Version.Branch b -> 
          let! branches = gitManager.FetchBranches url

          yield! 
            branches
            |> Seq.filter (fun x -> x.Name = b)
            |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Revision = x.Head })
            |> AsyncSeq.ofSeq

          for branch in branches |> Seq.map (fun x -> x.Name) do
            let! commits = gitManager.FetchCommits url branch
            yield!
              commits
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
      | _ -> 
        return new Exception("Only GitHub packages are supported") |> raise
    }

    member this.FetchManifest location = 
      match location with 
      | PackageLocation.GitHub g -> 
        let url = PackageLocation.gitHubUrl g.Package
        async {
          let! content = 
            gitManager.FetchFile url g.Revision Constants.ManifestFileName
          return 
            match Manifest.parse content with
            | Result.Ok manifest -> manifest
            | Result.Error errorMessage -> 
              new Exception("Invalid " + Constants.ManifestFileName + " file. \n" + errorMessage)
              |> raise
        }
      | _ -> 
        async {
          return new Exception("Only GitHub packages are supported") |> raise
        }

    member this.Prepare (location : PackageLocation) = async {
      match location with 
      | PackageLocation.GitHub g -> 
        let url = PackageLocation.gitHubUrl g.Package
        do! 
          gitManager.Clone url
          |> Async.Ignore
        do! 
          gitManager.Fetch url "master"
          |> Async.Ignore
        do! 
          gitManager.FetchTags url 
          |> Async.Ignore
        do! 
          gitManager.FetchBranches url 
          |> Async.Ignore
      | _ -> return ()
    }

    member this.FetchLock location = 
      match location with 
      | PackageLocation.GitHub g -> 
        let url = PackageLocation.gitHubUrl g.Package
        async {
          let url = PackageLocation.gitHubUrl g.Package
          let! content = 
            gitManager.FetchFile url g.Revision Constants.LockFileName 
          return 
            match Lock.parse content with
            | Result.Ok manifest -> manifest
            | Result.Error errorMessage -> 
              new Exception("Invalid " + Constants.LockFileName + " file. \n" + errorMessage)
              |> raise
        }
      | _ -> 
        async {
          return new Exception("Only GitHub packages are supported") |> raise
        }