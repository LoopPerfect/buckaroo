open System

type GitLocation = { URL : string; Revision : string }

[<EntryPoint>]
let main argv =
  // let v : Version.Version = Version.SemVerVersion { Major = 1; Minor = 2; Patch = 3; Increment = 0 }
  // let v2 : Version.Version = Version.BranchVersion "master"
  // Version.show v |> Console.WriteLine 
  // let p : Project.Project = { Owner = "LoopPerfect"; Project = "valuable" }
  // Project.show p |> Console.WriteLine 
  // let a : Atom.Atom = { Project = p; Version = v }
  // Atom.show a |> Console.WriteLine
  // let c = Constraint.Any [ Constraint.Exactly v; Constraint.Not (Constraint.Exactly v2) ]
  // Constraint.show c |> Console.WriteLine
  "1.2.3" 
    |> SemVer.parse 
    |> Option.map SemVer.show
    |> Option.defaultValue "?"
    |> Console.WriteLine 
  "4.5.6.78" 
    |> SemVer.parse 
    |> Option.map SemVer.show
    |> Option.defaultValue "?"
    |> Console.WriteLine 
  0 // return an integer exit code
