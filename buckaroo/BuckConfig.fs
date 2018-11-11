module Buckaroo.BuckConfig

open FS.INIReader 

let removeBuckarooEntries (config : INIAst.INIData) : INIAst.INIData = 
  match config |> Map.tryFind "repositories" with 
  | Some repositories -> 
    let repositoriesWithoutBuckarooCells = 
      repositories
      |> Map.filter (fun _ value -> 
        match value with 
        | INIAst.INIString path -> path.Contains("buckaroo") |> not
        | _ -> true
      )
    config |> Map.add "repositories" repositoriesWithoutBuckarooCells
  | None -> config

let addCells (cells : seq<string * string>) (config : INIAst.INIData) : INIAst.INIData = 
  let repositories = 
    config 
    |> Map.tryFind "repositories" 
    |> Option.defaultValue Map.empty
  let folder state next = 
    let (name, path) = next
    state 
    |> Map.add name (INIAst.INIValue.INIString path)
  let repositoriesWithCells = 
    cells
    |> Seq.fold folder repositories
  config |> Map.add "repositories" repositoriesWithCells

let rec renderValue (value : INIAst.INIValue) : string = 
  match value with 
  | INIAst.INIString s -> s
  | INIAst.INITuple xs -> xs |> Seq.map renderValue  |> String.concat ", "
  | INIAst.INIList xs -> xs |> Seq.map renderValue  |> String.concat ", "
  | INIAst.INIEmpty -> ""

let renderSection (section : Map<INIAst.INIKey, INIAst.INIValue>) : string = 
  section
  |> Seq.map (fun kvp -> "  " + kvp.Key + " = " + renderValue kvp.Value)
  |> String.concat "\n"

let render (config : INIAst.INIData) : string = 
  config 
  |> Seq.map (fun kvp -> "[" + kvp.Key + "]" + "\n" + (renderSection kvp.Value))
  |> String.concat "\n\n"
