module Dependency

open FParsec

type Project = Project.Project
type Constraint = Constraint.Constraint
type Target = Target.Target

type Dependency = { 
  Project : Project; 
  Constraint : Constraint; 
  Target : Target option
}

let show (x : Dependency) = 
  Project.show x.Project + "@" + Constraint.show x.Constraint + 
    (x.Target |> Option.map Target.show |> Option.defaultValue "")

let parser = parse {
  let! p = Project.parser
  do! CharParsers.skipString "@"
  let! c = Constraint.parser
  let! t = Target.parser |> Primitives.opt
  return 
    { Project = p; Constraint = c; Target = t }
}

let parse (x : string) : Option<Dependency> = 
  match run (parser .>> CharParsers.eof) x with
  | Success(result, _, _) -> Some result
  | Failure(errorMsg, _, _) -> None
