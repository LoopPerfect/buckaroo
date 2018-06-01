module Manifest

type Manifest = { Dependencies : List<Dependency.Dependency> }

// let freeVariables (xs : Seq<Dependency.Dependency>) = 
//   xs 
//   |> Seq.map (fun x -> x.Project)
//   |> Seq.distinct
//   |> Seq.toList
