namespace Buckaroo

type Manifest = { 
  Targets : Set<Target>; 
  Tags : Set<string>; 
  Dependencies : Set<Dependency>; 
}

module Manifest = 

  open ResultBuilder

  let zero : Manifest = {
    Targets = set [];
    Tags = set [];
    Dependencies = Set []
  }

  let remove (manifest : Manifest) (package : PackageIdentifier) = 
    {
      manifest with 
        Dependencies = 
          manifest.Dependencies
          |> Set.filter (fun d -> d.Package <> package); 
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
      |> Toml.get "package" 
      |> Option.bind Toml.asString 
      |> optionToResult "package must be specified for every dependency"
    let! version = 
      x 
      |> Toml.get "version" 
      |> Option.bind Toml.asString 
      |> optionToResult "version must be specified for every dependency"
    let! p = PackageIdentifier.parse name 
    let! c = Constraint.parse version
    let! ts = result {
      match x |> Toml.get "targets" with 
      | Some xs -> 
        let! array = 
          xs
          |> Toml.asArray
          |> optionToResult "targets must be an array"
        let! targets = 
          array.Items 
          |> Seq.map (fun item -> result {
            let! s = 
              item
              |> Toml.asString 
              |> optionToResult "targets must be an array of strings"
            return! 
              s 
              |> Target.parse
          })
          |> ResultBuilder.all
        return 
          targets 
          |> Seq.toList
          |> Some
      | None -> 
        return None
    }
    return { Package = p; Constraint = c; Targets = ts }
  }

  let parse (content : string) : Result<Manifest, string> = result {
    let! table = Toml.parse content |> Result.mapError (fun e -> e.Message)
    let tags : Set<string> = 
      table 
      |> Toml.get "tags" 
      |> Option.bind Toml.asArray 
      |> Option.map Toml.items
      |> Option.defaultValue []
      |> Seq.choose Toml.asString // TODO: Throw an error for invalid targets? 
      |> set 
    let! targets = 
      table 
      |> Toml.get "targets" 
      |> Option.bind Toml.asArray 
      |> Option.map Toml.items
      |> Option.defaultValue []
      |> Seq.choose Toml.asString // TODO: Throw an error for invalid targets? 
      |> Seq.map Target.parse
      |> all
    let! dependencies = 
      table.Values
      |> Seq.choose Toml.asTableArray
      |> Seq.collect (fun x -> x.Items)
      |> Seq.map tomlTableToDependency
      |> all
    return { 
      Targets = targets |> set; 
      Tags = tags; 
      Dependencies = dependencies |> Set.ofSeq; 
    }
  }

  let show (x : Manifest) : string = 
    x.Dependencies 
    |> Seq.map Dependency.show 
    |> Seq.sort
    |> String.concat "\n"

  let toToml (x : Manifest) : string = 
    (
      match x.Tags |> Seq.exists (fun _ -> true) with 
      | true -> 
        "tags = [ " + (
          x.Tags 
          |> Seq.distinct 
          |> Seq.sort 
          |> Seq.map (fun x -> "\"" + x + "\"")
          |> String.concat ", "
        ) + " ]\n\n"
      | false -> "" 
    ) + 
    (
      match x.Targets |> Seq.exists (fun _ -> true) with 
      | true -> 
        "targets = [ " + (
          x.Targets 
          |> Seq.distinct 
          |> Seq.sort 
          |> Seq.map (fun x -> "\"" + (Target.show x) + "\"")
          |> String.concat ", "
        ) + " ]\n\n"
      | false -> ""
    ) + 
    (
      x.Dependencies
      |> Seq.map (fun x -> 
        "[[dependency]]\n" + 
        "package = \"" + PackageIdentifier.show x.Package + "\"\n" + 
        "version = \"" + Constraint.show x.Constraint + "\"\n" + 
        (
          match x.Targets with
          | Some ts -> 
            "targets = [ " + 
            (ts |> Seq.map (fun t -> "\"" + Target.show t + "\"") |> String.concat ", ") + 
            " ]\n"
          | None -> ""
        )
      )
      |> String.concat "\n"
    )
