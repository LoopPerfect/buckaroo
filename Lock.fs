module Lock

open ResultBuilder

type Project = Project.Project
type ResolvedPackage = ResolvedPackage.ResolvedPackage
type Manifest = Manifest.Manifest
type Revision = string
type Location = string
type Solution = Solver.Solution

// TODO: Give this a better name
type ExactLocation = {
  Location : Location; 
  Revision : Revision;
}

type Lock = {
  Dependencies : Set<Project>;
  Packages : Map<Project, ExactLocation>; 
}

let show (x : Lock) : string = 
  x.Packages 
  |> Seq.map (fun p -> Project.show p.Key + "=" + p.Value.Location + "@" + p.Value.Revision)
  |> Seq.sort
  |> String.concat "\n"

let fromManifestAndSolution (manifest : Manifest) (solution : Solution) : Lock = 
  let dependencies = 
    manifest.Dependencies
    |> Seq.map (fun x -> x.Project)
    |> Set.ofSeq
  let packages = 
    solution
    |> Map.map (fun k v -> { Location = Project.sourceLocation k; Revision = v.Revision })
  { Dependencies = dependencies; Packages = packages }

let toToml (lock : Lock) = 
  "dependencies = [ " + 
    (lock.Dependencies 
      |> Seq.map (fun x -> "\"" + (Project.show x) + "\"")
      |> String.concat ", ") + 
    " ]\n\n" + 
    (
      lock.Packages
      |> Seq.map(fun x -> 
        let project = x.Key
        let exactLocation = x.Value
        "[[lock]]\n" + 
        "name = \"" + (Project.show project) + "\"\n" + 
        "location = \"" + exactLocation.Location + "\"\n" + 
        "revision = \"" + exactLocation.Revision + "\"\n"
        )
      |> String.concat "\n"
    )

let tomlTableToLockedPackage (x : Nett.TomlTable) : Result<(Project * ExactLocation), string> = result {
  let! name = 
    x 
    |> Toml.get "name" 
    |> Option.bind Toml.asString 
    |> optionToResult "name must be specified for every dependency"
  let! location = 
    x 
    |> Toml.get "location" 
    |> Option.bind Toml.asString 
    |> optionToResult "location must be specified for every dependency"
  let! revision = 
    x 
    |> Toml.get "revision" 
    |> Option.bind Toml.asString 
    |> optionToResult "revision must be specified for every dependency"
  let! project = Project.parse name 
  return (project, { Location = location; Revision = revision })
}

let parse (content : string) : Result<Lock, string> = result {
  let! table = Toml.parse content |> Result.mapError (fun e -> e.Message)
  let! lockedPackages = 
    table.Rows
    |> Seq.filter (fun x -> x.Key = "lock")
    |> Seq.choose (fun x -> Toml.asTableArray x.Value)
    |> Seq.collect (fun x -> x.Items)
    |> Seq.map tomlTableToLockedPackage
    |> ResultBuilder.all
  // TODO: If a project has more than one revision or location throw an error
  let packages = 
    lockedPackages
    |> Map.ofSeq
  let! dependenciesElement = 
    table 
    |> Toml.get "dependencies"
    |> optionToResult "dependencies element not found"
  let! dependenciesArray = 
    dependenciesElement
    |> Toml.asArray
    |> optionToResult "dependencies must be an array"
  let! dependenciesStrings = 
    dependenciesArray.Items
    |> Seq.map (
      Toml.asString 
      >> optionToResult "every dependency element must be a string")
    |> all
  let! dependenciesProjects = 
    dependenciesStrings
    |> Seq.map Project.parse
    |> all
  let dependencies = 
    dependenciesProjects
    |> Set.ofList
  return { Dependencies = dependencies; Packages = packages }
}
