namespace Buckaroo

open Buckaroo.Toml

type Manifest = {
  Targets : Set<Target>
  Tags : Set<string>
  Dependencies : Set<Dependency>
  PrivateDependencies : Set<Dependency>
  Locations : Map<AdhocPackageIdentifier, PackageSource>
  Overrides : Map<PackageIdentifier, string>
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
| InvalidTarget of string
| Dependency of DependencyParseError
| Location of LocationParseError
| ConflictingLocations of AdhocPackageIdentifier * PackageSource * PackageSource
| ConflictingOverrides of PackageIdentifier * string * string

module Manifest =

  open FSharpx.Result

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
      | ManifestParseError.TomlError e -> TomlError.show e
      | InvalidTarget t -> "Invalid target. " + t
      | Dependency d -> DependencyParseError.show d
      | Location l -> LocationParseError.show l
      | ConflictingLocations (p, a, b) ->
        "Conflicting locations found for " +
        (PackageIdentifier.show (Adhoc p)) + ": [ " +
        (PackageSource.show a) + ", " +
        (PackageSource.show b) + " ]"
      | ConflictingOverrides (p, a, b) ->
        "Conflicting overrides found for " +
        (PackageIdentifier.show p) + ": [ " + a + ", " + b + " ]"

  let zero : Manifest =
    {
      Targets = Set.empty
      Tags = Set.empty
      Dependencies = Set.empty
      PrivateDependencies = Set.empty
      Locations = Map.empty
      Overrides = Map.empty
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

  let private tomlTableToGitPackageSource (x : Nett.TomlObject) = result {
    let! uri =
      x
      |> Toml.asString
      |> Result.mapError LocationParseError.TomlError
    return PackageSource.Git {Uri = uri}
  }

  let private tomlTableToHttpPackageSource (x : Nett.TomlTable) = result {
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

    return PackageSource.Http (Map.ofSeq ([
        (version, { Url = url; StripPrefix = stripPrefix; Type = archiveType; })
    ]))
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

    let! packageSource =
      match x |> Toml.get("git") with
      | Result.Ok uri -> tomlTableToGitPackageSource uri
      | Result.Error _ -> tomlTableToHttpPackageSource x


    return (package, packageSource)
  }

  let tomlToTags (toml : Nett.TomlObject) =
    toml
    |> Toml.asArray
    |> Result.mapError ManifestParseError.TomlError
    |> Result.bind (
      Toml.items
      >> Seq.map (Toml.asString >> Result.mapError ManifestParseError.TomlError)
      >> Result.all
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
      |> Result.all

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
      |> Result.all
      |> Result.map (Seq.collect (fun x -> x.Items))
      |> Result.bind
        (Seq.map (tomlTableToDependency >> Result.mapError ManifestParseError.Dependency) >> Result.all)

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
      |> Result.all
      |> Result.map (Seq.collect (fun x -> x.Items))
      |> Result.bind
        (Seq.map (tomlTableToPackageSource >> Result.mapError ManifestParseError.Location) >> Result.all)

    let! locations =
      locationEntries
      |> Seq.fold
        (fun state next -> result {
          let! s = state
          let (package, source) = next
          match s |> Map.tryFind package with
          | Some existingSource ->
            return!
              if source = existingSource
              then
                state
              else
                ManifestParseError.ConflictingLocations (package, source, existingSource)
                |> Result.Error
          | None ->
            return
              s
              |> Map.add package source
        })
        (Ok Map.empty)

    let! overrideEntries =
      table
      |> Toml.tryGet "override"
      |> Option.map (fun tables ->
        tables
        |> Toml.asTableArray
        |> Result.bind (fun x ->
          x.Items
          |> Seq.map Override.fromToml
          |> Result.all
        )
        |> Result.mapError ManifestParseError.TomlError
      )
      |> Option.defaultValue (Result.Ok [])

    let! overrides =
      overrideEntries
      |> Seq.fold
        (fun state next -> result {
          let! s = state
          let nextOverride = next
          match s |> Map.tryFind nextOverride.Package with
          | Some substitution ->
            return!
              if nextOverride.Substitution = substitution
              then
                state
              else
                (nextOverride.Package, nextOverride.Substitution, substitution)
                |> ManifestParseError.ConflictingOverrides
                |> Result.Error
          | None ->
            return
              s
              |> Map.add nextOverride.Package nextOverride.Substitution
        })
        (Ok Map.empty)

    return {
      Targets = targets |> set
      Tags = tags
      Dependencies = dependencies
      PrivateDependencies = privateDependencies
      Locations = locations
      Overrides = overrides
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
      x.Locations
      |> Map.toSeq
      |> Seq.sortBy fst
      |> Seq.map (fun (package, source) ->
        (
          match source with
          | PackageSource.Git git ->
              "[[location]]\n" +
              "package = \"" + PackageIdentifier.show (PackageIdentifier.Adhoc package) + "\"\n" +
              "git = \"" + git.Uri + "\"\n"
          | PackageSource.Http http ->
              http
                |> Map.toSeq
                |> Seq.sortBy fst
                |> Seq.map (fun (version, h) ->
                  "[[location]]\n" +
                  "package = \"" + PackageIdentifier.show (PackageIdentifier.Adhoc package) + "\"\n" +
                  "version = \"" + (Version.show version) + "\"\n" +
                  "url = \"" + h.Url + "\"\n" +
                  (h.StripPrefix
                    |> Option.map(fun p -> "strip_prefix = \"" + p + "\"\n")
                    |> Option.defaultValue("")) +
                  (h.Type
                    |> Option.map(fun t -> "type = \"" + t.ToString() + "\"\n")
                    |> Option.defaultValue(""))
                )
                |> String.concat "\n"
        ) + "\n"
      )
      |> String.concat ""
    ) +
    (
      Seq.append
        (x.Dependencies
          |> Seq.map(fun x -> (false, x)))
        (x.PrivateDependencies
          |> Seq.map(fun x -> (true, x)))
      |> Seq.map (fun (isPrivate, x) ->
        "[[dependency]]\n" +
        "package = \"" + PackageIdentifier.show x.Package + "\"\n" +
        "version = \"" + Constraint.show x.Constraint + "\"\n" +
        (if isPrivate then "private = true\n" else "") +
        (
          match x.Targets with
          | Some ts ->
            "targets = [ " +
            (ts |> Seq.map (fun t -> "\"" + Target.show t + "\"") |> String.concat ", ") +
            " ]\n"
          | None -> ""
        ) +
        "\n"
      )
      |> String.concat ""
    ) +
    (
      x.Overrides
      |> Map.toSeq
      |> Seq.collect (fun (package, substitution) ->
        [
          "[[override]]"
          "package = \"" + PackageIdentifier.show package + "\""
          "substitution = \"" + substitution + "\""
          ""
        ]
      )
      |> String.concat "\n"
    )

  let hash manifest =
    manifest
    |> toToml
    |> Hashing.sha256
