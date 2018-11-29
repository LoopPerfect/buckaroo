namespace Buckaroo

type Manifest = { 
  Targets : Set<Target>; 
  Tags : Set<string>; 
  Dependencies : Set<Dependency>; 
  Locations : Map<AdhocPackageIdentifier * Version, PackageSource>; 
}

type DependencyParseError =
| PackageNotSpecified
| InvalidPackage of string
| VersionNotSpecified
| InvalidConstraint of string
| InvalidTargets
| InvalidTarget of string

type LocationParseError = 
| PackageNotSpecified
| InvalidPackage of string
| VersionNotSpecified
| InvalidVersion of string
| InvalidStripPrefix
| UrlNotSpecified

type ManifestParseError = 
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
      | DependencyParseError.PackageNotSpecified -> "Package must be specified for all dependencies"
      | DependencyParseError.InvalidPackage e -> "Invalid package name: " + e
      | DependencyParseError.VersionNotSpecified -> "Version must be specified for all dependencies"
      | DependencyParseError.InvalidConstraint e -> "Invalid constraint: " + e
      | DependencyParseError.InvalidTargets -> "Targets must an array of strings"
      | DependencyParseError.InvalidTarget e -> "Invalid target: " + e

  module LocationParseError = 
    let show (x : LocationParseError) = 
      match x with 
      | PackageNotSpecified -> "Package must be specified for all locations"
      | InvalidPackage e -> "Invalid package name: " + e
      | VersionNotSpecified -> "Version must be specified for all locations"
      | InvalidVersion e -> "Invalid version: " + e
      | InvalidStripPrefix -> "Strip-prefix must be a string"
      | UrlNotSpecified -> "URL must be specified for all locations"

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
    Locations = Map.empty; 
  }

  let remove (manifest : Manifest) (package : PackageIdentifier) = 
    {
      manifest with 
        Dependencies = 
          manifest.Dependencies
          |> Set.filter (fun d -> d.Package <> package); 
    }

  let private tomlTableToDependency (x : Nett.TomlTable) : Result<Dependency, DependencyParseError> = result {
    let! name = 
      x 
      |> Toml.get "package" 
      |> Option.bind Toml.asString 
      |> optionToResult DependencyParseError.PackageNotSpecified
    let! version = 
      x 
      |> Toml.get "version" 
      |> Option.bind Toml.asString 
      |> optionToResult DependencyParseError.VersionNotSpecified
    let! p = 
      PackageIdentifier.parse name 
      |> Result.mapError DependencyParseError.InvalidPackage
    let! c = 
      Constraint.parse version
      |> Result.mapError DependencyParseError.InvalidConstraint
    let! ts = result {
      match x |> Toml.get "targets" with 
      | Some xs -> 
        let! array = 
          xs
          |> Toml.asArray
          |> optionToResult DependencyParseError.InvalidTargets
        let! targets = 
          array.Items 
          |> Seq.map (fun item -> result {
            let! s = 
              item
              |> Toml.asString 
              |> optionToResult DependencyParseError.InvalidTargets
            return! 
              s 
              |> Target.parse
              |> Result.mapError DependencyParseError.InvalidTarget
          })
          |> Result.all
        return 
          targets 
          |> Seq.toList
          |> Some
      | None -> 
        return None
    }
    return { Package = p; Constraint = c; Targets = ts }
  }

  let private tomlTableToPackageSource (x : Nett.TomlTable) = result {
    let! package = 
      x 
      |> Toml.get "package" 
      |> Option.bind Toml.asString 
      |> optionToResult LocationParseError.PackageNotSpecified
      |> Result.bind (
        PackageIdentifier.parseAdhocIdentifier 
        >> Result.mapError LocationParseError.InvalidPackage
      )

    let! version = 
      x 
      |> Toml.get "version" 
      |> Option.bind Toml.asString 
      |> optionToResult LocationParseError.VersionNotSpecified
      |> Result.bind (
        Version.parse
        >> (Result.mapError LocationParseError.InvalidVersion)
      )

    let! url = 
      x 
      |> Toml.get "url" 
      |> Option.bind Toml.asString 
      |> optionToResult LocationParseError.UrlNotSpecified

    let! stripPrefix = 
      x 
      |> Toml.get "strip_prefix" 
      |> Result.Ok
      |> Result.map (Option.map Toml.asString)
      |> Result.bind (optionToResult LocationParseError.InvalidStripPrefix)

    let packageSource = 
      PackageSource.Http { Url = url; StripPrefix = stripPrefix }

    return ((package, version), packageSource)
  }

  let tomlToTags (toml : Nett.TomlObject) = result {
    let! array = 
      toml
      |> Toml.asArray
      |> optionToResult ManifestParseError.InvalidTags
    let! tags = 
      array.Items
      |> Seq.map (fun item -> result {
        let! s = 
          item
          |> Toml.asString
          |> optionToResult ManifestParseError.InvalidTags
        return s
      })
      |> all
    return set tags
  }

  let tomlToTargets (toml : Nett.TomlObject) = result {
    let! array = 
      toml
      |> Toml.asArray
      |> optionToResult ManifestParseError.InvalidTargets
    let! tags = 
      array.Items
      |> Seq.map (fun item -> result {
        let! s = 
          item
          |> Toml.asString
          |> optionToResult ManifestParseError.InvalidTargets
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
      |> Result.mapError (fun e -> ManifestParseError.InvalidToml e.Message)

    let! tags = 
      match table |> Toml.get "tags" with
      | None -> Ok Set.empty
      | Some tags -> tomlToTags tags

    let! targets = 
      match table |> Toml.get "targets" with
      | None -> Ok Set.empty
      | Some targets -> tomlToTargets targets

    let! dependencies = 
      table.Rows
      |> Seq.choose (fun kvp -> 
        if kvp.Key = "dependency"
        then Some kvp.Value
        else None
      )
      |> Seq.map (Toml.asTableArray >> (optionToResult ManifestParseError.InvalidDependency))
      |> all
      |> Result.map (Seq.collect (fun x -> x.Items))
      |> Result.bind (Seq.map (tomlTableToDependency >> Result.mapError ManifestParseError.Dependency) >> all)

    let! locationEntries = 
      table.Rows
      |> Seq.choose (fun kvp -> 
        if kvp.Key = "location"
        then Some kvp.Value
        else None
      )
      |> Seq.map (Toml.asTableArray >> (optionToResult ManifestParseError.InvalidLocation))
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
      Dependencies = dependencies |> Set.ofSeq; 
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
