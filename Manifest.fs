module Manifest

open Newtonsoft.Json

open Dependency

type Manifest = { Dependencies : List<Dependency> }

// let freeVariables (xs : Seq<Dependency.Dependency>) = 
//   xs 
//   |> Seq.map (fun x -> x.Project)
//   |> Seq.distinct
//   |> Seq.toList

let parse (content : string) = 
  // let data = JObject.Parse content 
  let dependencies = []
    // data.["dependencies"] 
    // |> Seq.map (fun x -> )
    // |> Seq.toList
  Some { Dependencies = dependencies }
