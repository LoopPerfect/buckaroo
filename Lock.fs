module Lock

type Project = Project.Project
type Revision = string
type Location = string

type LockedPackage = {
  Project : Project; 
  Dependencies : Set<Project>; 
  Location : Location; 
  Revision : Revision;
}

type Lock = {
  Packages : Set<LockedPackage>; 
}

let fromSolution (packages : Set<ResolvedPackage>) : Lock = 
  {
    Packages = 
      packages 
      |> Seq.map (fun x -> { 
        Project = x.Project; 
        Location = Project.sourceLocation x.Project; 
        Revision = x.Revision; 
      })
      |> Set.ofSeq
  }

