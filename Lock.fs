module Lock

type Project = Project.Project
type ResolvedPackage = ResolvedPackage.ResolvedPackage
type Revision = string
type Location = string

type LockedPackage = {
  Project : Project; 
  Location : Location; 
  Revision : Revision;
}

type Lock = {
  Packages : Set<LockedPackage>; 
}

let fromSolution (packages : Set<ResolvedPackage>) : Lock = 
  let lockedPackages = 
    packages 
    |> Seq.map (fun x -> 
      { 
        Project = x.Project; 
        Location = Project.sourceLocation x.Project; 
        Revision = x.Revision; })
    |> Set.ofSeq; 
  { Packages = lockedPackages }
