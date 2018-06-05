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

let show (x : Lock) : string = 
  x.Packages 
  |> Seq.map (fun p -> Project.show p.Project + "=" + p.Location + "@" + p.Revision)
  |> Seq.sort
  |> String.concat "\n"

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
