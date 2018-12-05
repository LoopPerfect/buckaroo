namespace Buckaroo

open Buckaroo.Toml

type Manifest = { 
  Targets : Set<Target>; 
  Tags : Set<string>; 
  Dependencies : Set<Dependency>; 
  PrivateDependencies : Set<Dependency>;
  Locations : Map<AdhocPackageIdentifier * Version, PackageSource>; 
}

type DependencyParseError =
| TomlError of TomlError
| InvalidPackage of string
| InvalidConstraint of string
| InvalidTarget of string

type LocationParseError = 
| TomlError of TomlError
| InvalidPackage of string
| InvalidVersion of string
| ArchiveTypeParseError of ArchiveType.ParseError

type ManifestParseError = 
| TomlError of TomlError
| InvalidToml of string
| InvalidTags
| InvalidTargets
| InvalidTarget of string
| InvalidDependency
| Dependency of DependencyParseError
| InvalidLocation
| Location of LocationParseError
| ConflictingLocations of (AdhocPackageIdentifier * Version) * PackageSource * PackageSource

module Manifest = 

  open Buckaroo.Result

  module DependencyParseError = 
    let show (x : DependencyParseError) = 
      match x with 
      | DependencyParseError.TomlError e -> TomlError.show e
      | DependencyParseError.InvalidPackage e -> "Invalid package name: " + e
      | DependencyParseError.InvalidConstraint e -> "Invalid constraint: " + e
      | DependencyParseError.InvalidTarget e -> "Invalid target: " + e

  module LocationParseError = 
    let show (x : LocationParseError) = 
      match x with 
      | LocationParseError.TomlError e -> TomlError.show e
      | InvalidPackage e -> "Invalid package name: " + e
      | InvalidVersion e -> "Invalid version: " + e
      | ArchiveTypeParseError s -> "Archive type parse error: " + (string s)

  module ManifestParseError = 
    let show (x : ManifestParseError) = 
      match x with 
      | InvalidToml s -> "Invalid TOML: " + s
      | InvalidTags -> "Invalid tags. Tags must be an array of strings. "
      | InvalidTargets -> "Invalid targets. Targets must be an array of strings. "
      | InvalidTarget t -> "Invalid target. " + t
      | InvalidDependency -> "Dependencies must be a TOML table"
      | Dependency d -> DependencyParseError.show d
      | InvalidLocation -> "Locations must be a TOML table"
      | Location l -> LocationParseError.show l
      | ConflictingLocations ((p, v), a, b) -> 
        "Conflicting locations found for " + 
        (PackageIdentifier.show (Adhoc p)) + "@" + 
        (Version.show v) + ": [ " + 
        (PackageSource.show a) + ", " + 
        (PackageSource.show b) + " ]"

  let zero : Manifest = {
    Targets = Set.empty;
    Tags = Set.empty;
    Dependencies = Set.empty;
    PrivateDependencies = Set.empty;
    Locations = Map.empty; 
  }

  let remove (manifest : Manifest) (package : PackageIdentifier) = 
    {
      manifest with 
        Dependencies = 
          manifest.Dependencies
          |> Set.filter (fun d -> d.Package <> package); 
    }

  let private tomlTableToDependency (x : Nett.TomlTable) : Result<(bool * Dependency), DependencyParseError> = result {
    let! package = 
      x 
      |> Toml.get "package" 
      |> Result.bind Toml.asString 
      |> Result.mapError (DependencyParseError.TomlError)
      |> Result.bind (PackageIdentifier.parse >> (Result.mapError DependencyParseError.InvalidPackage))
    
    let! version = 
      x 
      |> Toml.get "version" 
      |> Result.bind Toml.asString
      |> Result.mapError (DependencyParseError.TomlError)
      |> Result.bind (Constraint.parse >> Result.mapError (DependencyParseError.InvalidConstraint))

    let! targets = result {
      match x |> Toml.tryGet "targets" with 
      | Some xs -> 
        let! array = 
          xs
          |> Toml.asArray
          |> Result.mapError DependencyParseError.TomlError
        let! targets = 
          array.Items 
          |> Seq.map (fun item -> result {
            let! s = 
              item
              |> Toml.asString 
              |> Result.mapError DependencyParseError.TomlError
            return! 
              s 
              |> Target.parse
              |> Result.mapError (DependencyParseError.InvalidTarget)
          })
          |> Result.all
        return 
          targets 
          |> Seq.toList
          |> Some
      | None -> 
        return None
    }

    let! isPrivate = 
      x 
      |> Toml.tryGet "private"
      |> Option.map (fun x -> 
        x 
        |> Toml.asBool 
        |> Result.mapError DependencyParseError.TomlError
      )
      |> Option.defaultValue (Ok false)
    

    return (isPrivate, { Package = package; Constraint = version; Targets = targets })
  }

  let private tomlTableToPackageSource (x : Nett.TomlTable) = result {
    let! package = 
      x 
      |> Toml.get "package" 
      |> Result.bind Toml.asString 
      |> Result.mapError LocationParseError.TomlError
      |> Result.bind (
        PackageIdentifier.parseAdhocIdentifier 
        >> Result.mapError LocationParseError.InvalidPackage
      )

    let! version = 
      x 
      |> Toml.get "version" 
      |> Result.bind Toml.asString 
      |> Result.mapError LocationParseError.TomlError
      |> Result.bind (
        Version.parse
        >> (Result.mapError LocationParseError.InvalidVersion)
      )

    let! url = 
      x 
      |> Toml.get "url" 
      |> Result.bind Toml.asString 
      |> Result.mapError LocationParseError.TomlError

    let! stripPrefix = 
      x 
      |> Toml.tryGet "strip_prefix" 
      |> Option.map (Toml.asString)
      |> Option.map (Result.map Option.Some)
      |> Option.map (Result.mapError LocationParseError.TomlError)
      |> Option.defaultValue (Ok Option.None)

    let! archiveType = 
      x 
      |> Toml.tryGet "type" 
      |> Option.map (Toml.asString)
      |> Option.map (Result.mapError LocationParseError.TomlError)
      |> Option.map (Result.bind (ArchiveType.parse >> Result.mapError LocationParseError.ArchiveTypeParseError))
      |> Option.map (Result.map Option.Some)
      |> Option.defaultValue (Ok Option.None)

    let packageSource = 
      PackageSource.Http { Url = url; StripPrefix = stripPrefix; Type = archiveType }

    return ((package, version), packageSource)
  }

  let tomlToTags (toml : Nett.TomlObject) = 
    toml
    |> Toml.asArray
    |> Result.mapError ManifestParseError.TomlError
    |> Result.bind (
      Toml.items 
      >> Seq.map (Toml.asString >> Result.mapError ManifestParseError.TomlError) 
      >> all
    )
    |> Result.map set

  let tomlToTargets (toml : Nett.TomlObject) = result {
    let! array = 
      toml
      |> Toml.asArray
      |> Result.mapError ManifestParseError.TomlError

    let! tags = 
      array.Items
      |> Seq.map (fun item -> result {
        let! s = 
          item
          |> Toml.asString
          |> Result.mapError ManifestParseError.TomlError

        return! 
          s 
          |> Target.parse
          |> Result.mapError ManifestParseError.InvalidTarget
      })
      |> all

    return set tags
  }

  let parse (content : string) : Result<Manifest, ManifestParseError> = result {
    let! table = 
      Toml.parse content 
      |> Result.mapError ManifestParseError.TomlError

    let! tags = 
      table 
      |> Toml.tryGet "tags"
      |> Option.map tomlToTags
      |> Option.defaultValue (Ok Set.empty)

    let! targets = 
      table 
      |> Toml.tryGet "targets"
      |> Option.map tomlToTargets
      |> Option.defaultValue (Ok Set.empty)

    let! allDependencies = 
      table.Rows
      |> Seq.choose (fun kvp -> 
        if kvp.Key = "dependency"
        then Some kvp.Value
        else None
      )
      |> Seq.map (Toml.asTableArray >> (Result.mapError ManifestParseError.TomlError))
      |> all
      |> Result.map (Seq.collect (fun x -> x.Items))
      |> Result.bind (Seq.map (tomlTableToDependency >> Result.mapError ManifestParseError.Dependency) >> all)

    let dependencies =
      allDependencies
      |> Seq.choose (fun (isPrivate, dependency) -> 
        match isPrivate with
        | true -> None
        | false -> Some dependency
      ) 
      |> Set.ofSeq

    let privateDependencies =
      allDependencies
      |> Seq.choose (fun (isPrivate, dependency) -> 
        match isPrivate with
        | true -> Some dependency
        | false -> None
      ) 
      |> Set.ofSeq

    let! locationEntries = 
      table.Rows
      |> Seq.choose (fun kvp -> 
        if kvp.Key = "location"
        then Some kvp.Value
        else None
      )
      |> Seq.map (Toml.asTableArray >> (Result.mapError ManifestParseError.TomlError))
      |> all
      |> Result.map (Seq.collect (fun x -> x.Items))
      |> Result.bind (Seq.map (tomlTableToPackageSource >> Result.mapError ManifestParseError.Location) >> all)

    let! locations = 
      locationEntries
      |> Seq.fold 
        (fun state next -> result {
          let! s = state
          let ((package, version), source) = next
          match s |> Map.tryFind (package, version) with
          | Some existingSource -> 
            return! 
              if source = existingSource
              then 
                state
              else
                ManifestParseError.ConflictingLocations ((package, version), source, existingSource)
                |> Result.Error 
          | None -> 
            return 
              Map.empty 
              |> Map.add (package, version) source
        })
        (Ok Map.empty)

    return { 
      Targets = targets |> set; 
      Tags = tags; 
      Dependencies = dependencies; 
      PrivateDependencies = privateDependencies; 
      Locations = locations; 
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
