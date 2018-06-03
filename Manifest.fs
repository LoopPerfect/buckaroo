module Manifest

open Newtonsoft.Json
open FParsec

open Dependency

type Manifest = { Dependencies : List<Dependency> }

// let freeVariables (xs : Seq<Dependency.Dependency>) = 
//   xs 
//   |> Seq.map (fun x -> x.Project)
//   |> Seq.distinct
//   |> Seq.toList


