module SourceManager

open System
open System.IO
open LibGit2Sharp

open Project
open Version
open Dependency
open Manifest

let clone (url : string) (target : string) = 
  async {
    let path = Repository.Clone(url, target)
    return path
  }

let fetchVersions (p : Project) = 
  async {
    let url = Project.sourceLocation p
    let target = Path.Combine(Path.GetTempPath(), "buckaroo-" + Path.GetRandomFileName())
    let! gitPath = clone url target
    use repo = new Repository(gitPath)
    let branches = 
      repo.Branches
        |> Seq.filter (fun b -> b.IsRemote)
        |> Seq.map (fun b -> b.RemoteName)
        |> Seq.distinct
        |> Seq.map (fun b -> Version.BranchVersion b)
    let tags = 
      repo.Tags 
        |> Seq.map (fun t -> t.FriendlyName)
        |> Seq.map (fun b -> Version.TagVersion b)
    let semVers = 
      repo.Tags 
        |> Seq.map (fun t -> SemVer.parse t.FriendlyName)
        |> Seq.collect 
          (fun m -> 
            match m with 
            | Some x -> [ x ]
            | None -> [])
        |> Seq.map (fun v -> Version.SemVerVersion v)
    return branches 
      |> Seq.append tags 
      |> Seq.append semVers 
      |> Seq.toList
  }

let fetchRevisions (project : Project) (version : Version) = 
  async {
    let url = Project.sourceLocation project
    let target = Path.Combine(Path.GetTempPath(), "buckaroo-" + Path.GetRandomFileName())
    let! gitPath = clone url target
    use repo = new Repository(gitPath)
    return 
      match version with 
      | Version.SemVerVersion semVer -> 
        repo.Tags 
          |> Seq.filter (fun t -> SemVer.parse t.FriendlyName = Some semVer)
          |> Seq.map (fun t -> t.Target.Sha)
          |> Seq.distinct
          |> Seq.toList
      | Version.BranchVersion b -> 
        let branch = 
          repo.Branches
            |> Seq.filter (fun x -> x.FriendlyName = b)
            |> Seq.item 0
        Commands.Checkout(repo, branch) |> ignore
        branch.Commits
          |> Seq.map (fun c -> c.Sha)
          |> Seq.distinct
          |> Seq.toList
      | Version.RevisionVersion r -> 
        repo.Commits
          |> Seq.map (fun c -> c.Sha)
          |> Seq.filter (fun c -> c = r)
          |> Seq.take 1 
          |> Seq.toList
      | Version.TagVersion tag -> 
        repo.Tags 
          |> Seq.filter (fun t -> t.FriendlyName = tag)
          |> Seq.map (fun t -> t.Target.Sha)
          |> Seq.distinct
          |> Seq.toList
  }

let fetchManifest (project : Project.Project) (revision : string) = 
  async {
    let url = Project.sourceLocation project
    let target = Path.Combine(Path.GetTempPath(), "buckaroo-" + Path.GetRandomFileName())
    let! gitPath = clone url target
    use repo = new Repository(gitPath)
    Commands.Checkout(repo, revision) |> ignore
    let blob = repo.Head.Tip.["readme.md"].Target :?> Blob;
    let content = blob.GetContentText()
    // return content
    return { Dependencies = [] }
  }
