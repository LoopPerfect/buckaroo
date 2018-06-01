module SourceManager

open System
open System.IO
open LibGit2Sharp

let clone (url : string) (target : string) = 
  async {
    let path = Repository.Clone(url, target)
    return path
  }

let fetchVersions (p : Project.Project) = 
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
