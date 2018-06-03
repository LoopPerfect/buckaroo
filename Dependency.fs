module Dependency

open Project
open Constraint

type Dependency = { 
  Project : Project; 
  Constraint : Constraint 
}

let show (x : Dependency) = 
  Project.show x.Project + "@" + Constraint.show x.Constraint
