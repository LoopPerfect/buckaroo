namespace Buckaroo

type Lock = {
  Dependencies : Set<TargetIdentifier>; 
  Packages : Map<PackageIdentifier, PackageLocation>; 
}

// let show (x : Lock) : string = 
//   x.Packages 
//   |> Seq.map (fun p -> PackageIdentifier.show p.Key + "=" + p.Value.Location + "@" + p.Value.Revision)
//   |> Seq.sort
//   |> String.concat "\n"

module Lock = 

  open ResultBuilder

  let fromManifestAndSolution (manifest : Manifest) (solution : Solution) : Lock = 
    let dependencies = 
      manifest.Dependencies
      |> Seq.map (fun x -> x.Package)
      |> Seq.collect (fun p -> 
        solution 
        |> Map.tryFind p 
        |> Option.map (fun x -> 
          x.Manifest.Targets 
          |> Seq.map (fun t -> { Package = p; Target = t})
        )
        |> Option.defaultValue Seq.empty
      )
      |> Set.ofSeq
    let packages = 
      solution
      |> Map.map (fun _ v -> v.Location)
    { Dependencies = dependencies; Packages = packages }

  let toToml (lock : Lock) = 
    (
      lock.Dependencies
      |> Seq.map(fun x -> 
        "[[dependency]]\n" + 
        "package = \"" + (PackageIdentifier.show x.Package) + "\"\n" + 
        "target = \"" + x.Target + "\"\n" 
      )
      |> String.concat "\n"
    ) + 
    (
      lock.Packages
      |> Seq.map(fun x -> 
        let package = x.Key
        let exactLocation = x.Value
        "[[lock]]\n" + 
        "name = \"" + (PackageIdentifier.show package) + "\"\n" + 
        match exactLocation with 
        | Git git -> 
          "url = \"" + git.Url + "\"\n" + 
          "revision = \"" + git.Revision + "\"\n"
        | Http http -> 
          "url = \"" + http.Url + "\"\n" + 
          "type = \"" + (ArchiveType.show http.Type) + "\"\n" + 
          "strip_refix = \"" + http.StripPrefix + "\"\n"
        | GitHub gitHub -> 
          "service = \"github\"\n" + 
          "owner = \"" + gitHub.Package.Owner + "\"\n" + 
          "project = \"" + gitHub.Package.Project + "\"\n" + 
          "revision = \"" + gitHub.Revision + "\"\n"
      )
      |> String.concat "\n"
    )

  let tomlTableToLockedPackage (x : Nett.TomlTable) : Result<(PackageIdentifier * PackageLocation), string> = result {
    let! name = 
      x 
      |> Toml.get "name" 
      |> Option.bind Toml.asString 
      |> optionToResult "name must be specified for every dependency"
    let! url = 
      x 
      |> Toml.get "url" 
      |> Option.bind Toml.asString 
      |> optionToResult "location must be specified for every dependency"
    let! revision = 
      x 
      |> Toml.get "revision" 
      |> Option.bind Toml.asString 
      |> optionToResult "revision must be specified for every dependency"
    let! packageIdentifier = PackageIdentifier.parse name 
    return (packageIdentifier, Git { Url = url; Revision = revision })
  }

  let tomlTableToTargetIdentifier (x : Nett.TomlTable) : Result<TargetIdentifier, string> = result {
    let! package = 
      x 
      |> Toml.get "package" 
      |> Option.bind Toml.asString 
      |> optionToResult "package must be specified for every dependency"
      |> Result.bind PackageIdentifier.parse
    let! target = 
      x 
      |> Toml.get "target" 
      |> Option.bind Toml.asString 
      |> optionToResult "target must be specified for every dependency"
    return { Package = package; Target = target }
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
    let! dependencies = 
      table.Rows
      |> Seq.filter (fun x -> x.Key = "dependency")
      |> Seq.choose (fun x -> Toml.asTableArray x.Value)
      |> Seq.collect (fun x -> x.Items)
      |> Seq.map tomlTableToTargetIdentifier
      |> ResultBuilder.all
    return { Dependencies = set dependencies; Packages = packages }
  }
