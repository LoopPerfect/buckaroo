module Dependency

open FParsec

type Project = Project.Project
type Constraint = Constraint.Constraint

type Dependency = { 
  Project : Project; 
  Constraint : Constraint 
}

let show (x : Dependency) = 
  Project.show x.Project + "@" + Constraint.show x.Constraint

let parser = parse {
  let! p = Project.parser
  do! CharParsers.skipString "@"
  let! c = Constraint.parser
  return 
    { Project = p; Constraint = c }
}

let parse (x : string) : Option<Dependency> = 
  match run parser x with
  | Success(result, _, _) -> Some result
  | Failure(errorMsg, _, _) -> None
