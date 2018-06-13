module Manifest

open Dependency
open ResultBuilder

type Dependency = Dependency.Dependency

type Manifest = { 
  Target : Option<string>; 
  Tags : Set<string>; 
  Dependencies : Set<Dependency>
}

let zero = {
  Target = None;
  Tags = set [];
  Dependencies = Set []
}

let normalizeTarget (target : string) = 
  let trimmed = target.Trim()
  if trimmed.StartsWith("//")
  then trimmed
  else 
    if trimmed.StartsWith(":")
    then "//" + trimmed
    else "//:" + trimmed

let tomlTableToDependency (x : Nett.TomlTable) : Result<Dependency, string> = result {
  let! name = 
    x 
    |> Toml.get "name" 
    |> Option.bind Toml.asString 
    |> optionToResult "name must be specified for every dependency"
  let! version = 
    x 
    |> Toml.get "version" 
    |> Option.bind Toml.asString 
    |> optionToResult "version must be specified for every dependency"
  let! p = Project.parse name 
  let! c = Constraint.parse version
  return { Project = p; Constraint = c }
}

let parse (content : string) : Result<Manifest, string> = result {
  let! table = Toml.parse content |> Result.mapError (fun e -> e.Message)
  let target = 
    table 
    |> Toml.get "target" 
    |> Option.bind Toml.asString 
  let! dependencies = 
    table.Values
    |> Seq.choose Toml.asTableArray
    |> Seq.collect (fun x -> x.Items)
    |> Seq.map tomlTableToDependency
    |> all
  return { Target = target; Tags = set []; Dependencies = dependencies |> Set.ofSeq }
}

let show (x : Manifest) : string = 
  x.Dependencies 
  |> Seq.map Dependency.show 
  |> Seq.sort
  |> String.concat "\n"
