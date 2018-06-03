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

let parse (x : string) : Option<Manifest> = 
  match run parser x with
  | Success(result, _, _) -> Some result
  | Failure(errorMsg, _, _) -> 
    System.Console.WriteLine errorMsg
    None

let show (x : Manifest) : string = 
  x.Dependencies 
  |> Seq.map Dependency.show 
  |> String.concat "\n"

// let freeVariables (xs : Seq<Dependency.Dependency>) = 
//   xs 
//   |> Seq.map (fun x -> x.Project)
//   |> Seq.distinct
//   |> Seq.toList


