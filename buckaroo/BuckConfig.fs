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

let addCells cells (config : INIAst.INIData) : INIAst.INIData = 
  let repositories = 
    config 
    |> Map.tryFind "repositories" 
    |> Option.defaultValue Map.empty
  let folder state next = 
    let (name, path) = next
    state |> Map.add name path
  let repositoriesWithCells = 
    cells
    |> Seq.fold folder repositories
  config |> Map.add "repositories" repositoriesWithCells

let render (config : INIAst.INIData) : string = 
  config 
  |> Seq.map (fun kvp -> "[" + kvp.Key + "]")
  |> String.concat "\n\n"
