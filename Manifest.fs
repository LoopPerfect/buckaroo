module Manifest

type Dependency = Dependency.Dependency

type Manifest = { 
  Tags : Set<string>; 
  Dependencies : Set<Dependency>
}

let tomlTableToDependency (x : Nett.TomlTable) : Dependency = 
  let name = Toml.asString x.["name"] |> Option.get
  let version = Toml.asString x.["version"] |> Option.get
  let p = 
    name 
    |> Project.parse
    |> Option.get
  let c = 
    version
    |> Constraint.parse
    |> Option.get
  { Project = p; Constraint = c }

let parse (x : string) : Result<Manifest, string> = 
  let table = Toml.parse x
  try 
    match table with 
    | Result.Ok t -> 
      let dependencies = 
        t.Values
        |> Seq.choose Toml.asTableArray
        |> Seq.collect (fun x -> x.Items)
        |> Seq.map tomlTableToDependency
        |> Set.ofSeq
      Result.Ok { Tags = set []; Dependencies = dependencies }
    | Result.Error e -> Result.Error e.Message
  with 
  | e -> Result.Error e.Message

let show (x : Manifest) : string = 
  x.Dependencies 
  |> Seq.map Dependency.show 
  |> Seq.sort
  |> String.concat "\n"
