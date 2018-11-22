namespace Buckaroo

open System
open System.IO
open System.Security.Cryptography
open System.Text.RegularExpressions
open FSharp.Control
open LibGit2Sharp
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

    member this.Prepare (dependency : Dependency) = 
      let rec prepare dependency = 
        async {
          match dependency.Constraint with 
          | Constraint.Exactly version -> 
            let! url = gitUrl dependency.Package
            let! gitPath = 
              gitManager.Clone url
            match version with 
            | Version.Branch branch -> 
              do! gitManager.Fetch url branch
              return ()
            | _ -> return ()
          | _ -> 
            let! url = gitUrl dependency.Package
            let! gitPath = 
              gitManager.Clone url
            return ()
        }
      prepare dependency

    member this.FetchVersions package = asyncSeq {
      let! url = gitUrl package
      let! gitPath = 
        gitManager.Clone url 
      use repo = new Repository(gitPath)

      let references = repo.Network.ListReferences(url)

      // Tags and sem-vers
      for reference in references do 
        if reference.IsTag
        then
          let tag = reference.CanonicalName.Substring("refs/tags/".Length)
          match SemVer.parse tag with
          | Result.Ok semVer -> 
            yield semVer |> Version.SemVerVersion
            yield tag |> Version.Tag
          | Result.Error _ -> 
            yield tag |> Version.Tag
        else ()

      let branches = 
        references
        |> Seq.filter (fun x -> x.IsLocalBranch && not x.IsTag)
        |> Seq.map (fun x -> x.CanonicalName.Substring("refs/heads/".Length))
        |> Seq.distinct
        |> Seq.sortBy branchPriority
        |> Seq.toList

      // Branches
      yield! 
        branches 
        |> Seq.map Version.Branch
        |> AsyncSeq.ofSeq

      // Revisions
      for branch in branches do 
        do! gitManager.Fetch url branch
        let cf = new CommitFilter()
        cf.IncludeReachableFrom <- "origin/" + branch
        yield! 
          repo.Commits.QueryBy(cf)
          |> Seq.sortByDescending (fun c -> c.Committer.When)
          |> Seq.map (fun x -> x.Sha)
          |> Seq.distinct
          |> Seq.map Buckaroo.Version.Revision
          |> AsyncSeq.ofSeq
    }

    member this.FetchLocations package version = asyncSeq {
      match package with 
      | PackageIdentifier.GitHub g -> 
        let url = PackageLocation.gitHubUrl g
        let! gitPath = 
          gitManager.Clone url
        use repo = new Repository(gitPath)
        match version with 
        | Buckaroo.SemVerVersion semVer -> 
          yield! 
            repo.Tags 
            |> Seq.filter (fun t -> SemVer.parse t.FriendlyName = Result.Ok semVer)
            |> Seq.map (fun t -> t.Target.Sha)
            |> Seq.distinct
            |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Revision = x })
            |> AsyncSeq.ofSeq
        | Buckaroo.Version.Branch b -> 
          yield! 
            repo.Branches
            |> Seq.filter (fun x -> x.FriendlyName = "origin/" + b)
            |> Seq.collect (fun branch -> 
              branch.Commits
              |> Seq.sortByDescending (fun c -> c.Committer.When)
              |> Seq.map (fun c -> c.Sha)
              |> Seq.distinct
              |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Revision = x })
            )
            |> AsyncSeq.ofSeq
        | Buckaroo.Version.Revision r -> 
          yield! 
            repo.Commits
            |> Seq.map (fun c -> c.Sha)
            |> Seq.filter (fun c -> c = r)
            |> Seq.truncate 1 
            |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Revision = x })
            |> AsyncSeq.ofSeq
        | Buckaroo.Version.Tag tag -> 
          yield! 
            repo.Tags 
            |> Seq.filter (fun t -> t.FriendlyName = tag)
            |> Seq.map (fun t -> t.Target.Sha)
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
          let url = PackageLocation.gitHubUrl g.Package
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