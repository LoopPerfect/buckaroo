module Manifest

open Newtonsoft.Json
open FParsec

type Dependency = Dependency.Dependency

type Manifest = { Dependencies : List<Dependency> }

let parser = parse {
  do! CharParsers.spaces
  let! deps = Primitives.sepEndBy Dependency.parser CharParsers.spaces1
  return { Dependencies = deps }
}

let parse x = 
  match run parser x with
  | Success(result, _, _) -> Result.Ok result
  | Failure(error, _, _) -> Result.Error error

let show (x : Manifest) : string = 
  x.Dependencies 
  |> Seq.map Dependency.show 
  |> String.concat "\n"

// let freeVariables (xs : Seq<Dependency.Dependency>) = 
//   xs 
//   |> Seq.map (fun x -> x.Project)
//   |> Seq.distinct
//   |> Seq.toList


