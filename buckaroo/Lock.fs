namespace Buckaroo

type LockedPackage = {
  Versions : Set<Version>;
  Location : PackageLocation;
  PrivatePackages : Map<PackageIdentifier, LockedPackage>;
}

type Lock = {
  ManifestHash : string;
  Dependencies : Set<TargetIdentifier>;
  Packages : Map<PackageIdentifier, LockedPackage>;
}

module Lock =

  open Buckaroo.Result

  module LockedPackage =
    let show (x : LockedPackage) =

      let rec f (x : LockedPackage) (depth : int) =
        let indent = "-" |> String.replicate depth
        let indent2 = "-" |> String.replicate (depth + 1)
        indent + (Version.show x.Versions.MinimumElement) + "@" + (PackageLocation.show x.Location) +
        (if x.Versions.Count = 1
         then ""
         else
           indent2 + "aka: " +
           (x.Versions
            |> Set.toSeq
            |> Seq.tail
            |> Seq.map Version.show
            |> String.concat ", "
            |> (fun x -> "{" + x + "}"))) +
        (
          x.PrivatePackages
          |> Map.toSeq
          |> Seq.map (fun (k, v) -> f v (depth + 1))
          |> String.concat "\n"
        )
      f x 0

  let rec private flattenLockedPackage (parents : PackageIdentifier list) (package : LockedPackage) = seq {
    yield (parents, (package.Location, package.Versions))
    yield!
      package.PrivatePackages
      |> Map.toSeq
      |> Seq.collect (fun (k, v) -> flattenLockedPackage (parents @ [ k ]) v)
  }

  let private flattenLock (lock : Lock) = seq {
    for (package, lockedPackage) in lock.Packages |> Map.toSeq do
      yield! flattenLockedPackage [ package ] lockedPackage
  }

  let showDiff (before : Lock) (after : Lock) : string =
    let beforeFlat =
      before
      |> flattenLock
      |> Map.ofSeq

    let afterFlat =
      after
      |> flattenLock
      |> Map.ofSeq

    let additions =
      afterFlat
      |> Map.toSeq
      |> Seq.filter (fun (k, v) -> beforeFlat |> Map.containsKey k |> not)
      |> Seq.distinct

    let removals =
      beforeFlat
      |> Map.toSeq
      |> Seq.filter (fun (k, v) -> afterFlat |> Map.containsKey k |> not)
      |> Seq.distinct

    let changes =
      afterFlat
      |> Map.toSeq
      |> Seq.choose (fun (k, v) ->
        beforeFlat
        |> Map.tryFind k
        |> Option.bind (fun y ->
          if y <> v
          then Some (k, y, v)
          else None
        )
      )

    [
      "Added: ";
      (
        additions
        |> Seq.map (fun (k, (l, v)) ->
          "  " + (k |> Seq.map PackageIdentifier.show |> String.concat " ")// +
          //" -> " + (PackageLocation.show l) + "@" + (Version.show v)
        )
        |> String.concat "\n"
      );
      "Removed: ";
      (
        removals
        |> Seq.map (fun (k, (l, v)) ->
          "  " + (k |> Seq.map PackageIdentifier.show |> String.concat " ")// +
          //" -> " + (PackageLocation.show l) + "@" + (Version.show v)
        )
        |> String.concat "\n"
      );
      "Changed: ";
      (
        changes
        |> Seq.map (fun (k, (bl, bv), (al, av)) ->
          "  " + (k |> Seq.map PackageIdentifier.show |> String.concat " ") +
          " " + (PackageLocation.show bl) + "@" //+ (Version.show bv)// +
          //" -> " + (PackageLocation.show al) + "@" + (Version.show av)
        )
        |> String.concat "\n"
      );
    ]
    |> String.concat "\n"

  let fromManifestAndSolution (manifest : Manifest) (solution : Solution) : Lock =
    let manifestHash =
      manifest
      |> Manifest.toToml
      |> Hashing.sha256

    let dependencies =
      manifest.Dependencies
      |> Seq.append manifest.PrivateDependencies
      |> Seq.map (fun x -> x.Package)
      |> Seq.collect (fun p ->
        solution.Resolutions
        |> Map.tryFind p
        |> Option.map (fun (rv, _) ->
          rv.Manifest.Targets
          |> Seq.map (fun t -> { Package = p; Target = t})
        )
        |> Option.defaultValue Seq.empty
      )
      |> Set.ofSeq

    let rec extractPackages (solution : Solution) : Map<PackageIdentifier, LockedPackage> =
      solution.Resolutions
      |> Map.toSeq
      |> Seq.map (fun (k, (rv, s)) ->
        let lockedPackage = {
          Location = rv.Location;
          Versions = rv.Versions;
          PrivatePackages = extractPackages s;
        }

        (k, lockedPackage)
      )
      |> Map.ofSeq

    let packages =
      solution
      |> extractPackages

    { ManifestHash = manifestHash; Dependencies = dependencies; Packages = packages }

  let private quote x = "\"" + x + "\""

  let private lockKey parents =
    parents |> Seq.map (PackageIdentifier.show >> quote >> ((+) "lock.")) |> String.concat "."

  let toToml (lock : Lock) =
    (
       "manifest = \"" + lock.ManifestHash + "\"\n\n"
    ) +
    (
      lock.Dependencies
      |> Seq.map(fun x ->
        "[[dependency]]\n" +
        "package = \"" + (PackageIdentifier.show x.Package) + "\"\n" +
        "target = \"" + (Target.show x.Target) + "\"\n\n"
      )
      |> String.concat ""
    ) +
    (
      lock.Packages
      |> Map.toSeq
      |> Seq.collect (fun (k, v) -> flattenLockedPackage [ k ] v)
      |> Seq.map(fun x ->
        let (parents, (location, versions)) = x
        "[" + (lockKey parents) + "]\n" +
        "versions = [" +
        (versions
          |> Set.toSeq
          |> Seq.map Version.show
          |> Seq.map (fun x ->"\"" + x + "\"")
          |> String.concat ", ") +
        "]\n" +
        match location with
        | Git git ->
          "git = \"" + git.Url + "\"\n" +
          "revision = \"" + git.Revision + "\"\n"
        | Http http ->
          "url = \"" + http.Url + "\"\n" +
          "sha256 = \"" + (http.Sha256) + "\"\n" +
          (http.Type |> Option.map (fun x -> "type = \"" + (ArchiveType.show x) + "\"\n") |> Option.defaultValue "") +
          (http.StripPrefix |> Option.map (fun x -> "strip_prefix = \"" + x + "\"\n") |> Option.defaultValue "")
        | GitHub gitHub ->
          "revision = \"" + gitHub.Revision + "\"\n"
        | BitBucket bitBucket ->
          "revision = \"" + bitBucket.Revision + "\"\n"
        | GitLab gitLab ->
          "revision = \"" + gitLab.Revision + "\"\n"
      )
      |> String.concat "\n"
    )

  let private tomlTableToHttpLocation x = result {
    let! url =
      x
      |> Toml.get "url"
      |> Result.mapError Toml.TomlError.show
      |> Result.bind (Toml.asString >> Result.mapError Toml.TomlError.show)

    let! sha256 =
      x
      |> Toml.get "sha256"
      |> Result.mapError Toml.TomlError.show
      |> Result.bind (Toml.asString >> Result.mapError Toml.TomlError.show)

    let! stripPrefix =
      x
      |> Toml.tryGet "strip_prefix"
      |> Option.map (Toml.asString)
      |> Option.map (Result.map Option.Some)
      |> Option.map (Result.mapError Toml.TomlError.show)
      |> Option.defaultValue (Result.Ok Option.None)

    let! archiveType =
      x
      |> Toml.tryGet "type"
      |> Option.map (Toml.asString)
      |> Option.map (Result.mapError Toml.TomlError.show)
      |> Option.map (Result.bind (ArchiveType.parse >> Result.mapError ArchiveType.ParseError.show))
      |> Option.map (Result.map Option.Some)
      |> Option.defaultValue (Result.Ok Option.None)

    return PackageLocation.Http {
      Url = url;
      StripPrefix = stripPrefix;
      Type = archiveType;
      Sha256 = sha256;
    }
  }

  let private tomlTableToGitLocation x = result {
    let! uri =
      x
      |> Toml.get "git"
      |> Result.mapError Toml.TomlError.show
      |> Result.bind (Toml.asString >> Result.mapError Toml.TomlError.show)

    let! revision =
      x
      |> Toml.get "revision"
      |> Result.mapError Toml.TomlError.show
      |> Result.bind (Toml.asString >> Result.mapError Toml.TomlError.show)

    return PackageLocation.Git {
      Url = uri;
      Revision = revision
    }
  }

  let private tomlTableToAdhocLocation x = result {
    let git = tomlTableToGitLocation x
    let http = tomlTableToHttpLocation x

    return!
      match (git, http) with
      | (Result.Ok _, Result.Ok _) ->
        Result.Error ("ambigious lock entry")
      | (Result.Ok g, _) -> Result.Ok g
      | (_ , Result.Ok h) -> Result.Ok h
      | (Result.Error x, Result.Error y) ->
        Result.Error (
          "error parsing lock entry, it's neither a git nor http location because:\n"+
          "not git:\n"+ x +
          "\nnot http:\n" + y
        )
  }

  let private tomlTableToGitHubLocation packageIdentifier x = result {
    let! revision =
      x
      |> Toml.get "revision"
      |> Result.mapError Toml.TomlError.show
      |> Result.bind (Toml.asString >> Result.mapError Toml.TomlError.show)

    return
      PackageLocation.GitHub
        {
          Package = packageIdentifier;
          Revision = revision;
        }
  }

  let private tomlTableToBitBucketLocation packageIdentifier x = result {
    let! revision =
      x
      |> Toml.get "revision"
      |> Result.mapError Toml.TomlError.show
      |> Result.bind (Toml.asString >> Result.mapError Toml.TomlError.show)

    return
      PackageLocation.BitBucket
        {
          Package = packageIdentifier;
          Revision = revision;
        }
  }

  let private tomlTableToGitLabLocation packageIdentifier x = result {
    let! revision =
      x
      |> Toml.get "revision"
      |> Result.mapError Toml.TomlError.show
      |> Result.bind (Toml.asString >> Result.mapError Toml.TomlError.show)

    return
      PackageLocation.GitLab
        {
          Package = packageIdentifier;
          Revision = revision;
        }
  }

  let rec private tomlTableToLockedPackage packageIdentifier x = result {
    let! versions =
      x
      |> Toml.get "versions"
      |> Result.bind (Toml.asArray)
      |> Result.map (Toml.items)
      |> Result.map (List.map Toml.asString)
      |> Result.bind Result.all
      |> Result.mapError Toml.TomlError.show
      |> Result.map (List.map Version.parse)
      |> Result.bind Result.all
      |> Result.map Set.ofList

    let! location =
      match packageIdentifier with
      | PackageIdentifier.GitHub gitHub ->
        tomlTableToGitHubLocation gitHub x
      | PackageIdentifier.BitBucket bitBucket ->
        tomlTableToBitBucketLocation bitBucket x
      | PackageIdentifier.GitLab gitLab ->
        tomlTableToGitLabLocation gitLab x
      | PackageIdentifier.Adhoc _ ->
        tomlTableToAdhocLocation x

    let! lockEntries =
      x
      |> Toml.tryGet "lock"
      |> Option.map (Toml.asTable)
      |> Option.map (Result.map Toml.entries)
      |> Option.map (Result.mapError Toml.TomlError.show)
      |> Option.defaultValue (Result.Ok Seq.empty)

    let! privatePackages =
      lockEntries
      |> Seq.map (fun (k, v) -> result {
        let! packageIdentifier =
          k
          |> PackageIdentifier.parse

        let! lockedPackage =
          v
          |> Toml.asTable
          |> Result.mapError Toml.TomlError.show
          |> Result.bind (tomlTableToLockedPackage packageIdentifier)

        return (packageIdentifier, lockedPackage)
      })
      |> Result.all
      |> Result.map Map.ofSeq

    return {
      Versions = versions;
      Location = location;
      PrivatePackages = privatePackages;
    }
  }

  let tomlTableToTargetIdentifier (x : Nett.TomlTable) : Result<TargetIdentifier, string> = result {
    let! package =
      x
      |> Toml.get "package"
      |> Result.mapError Toml.TomlError.show
      |> Result.bind (Toml.asString >> Result.mapError Toml.TomlError.show)
      |> Result.bind PackageIdentifier.parse

    let! target =
      x
      |> Toml.get "target"
      |> Result.mapError Toml.TomlError.show
      |> Result.bind (Toml.asString >> Result.mapError Toml.TomlError.show)
      |> Result.bind Target.parse

    return { Package = package; Target = target }
  }

  let parse (content : string) : Result<Lock, string> = result {
    let! table =
      content
      |> Toml.parse
      |> Result.mapError Toml.TomlError.show

    let! manifestHash =
      table
      |> Toml.get "manifest"
      |> Result.mapError Toml.TomlError.show
      |> Result.bind (Toml.asString >> Result.mapError Toml.TomlError.show)

    let! lockEntries =
      table
      |> Toml.tryGet "lock"
      |> Option.map (Toml.asTable >> Result.map Toml.entries >> Result.mapError Toml.TomlError.show)
      |> Option.defaultValue (Result.Ok Seq.empty)

    let! packages =
      lockEntries
      |> Seq.map (fun (k, v) -> result {
        let! packageIdentifier =
          k
          |> PackageIdentifier.parse

        let! lockedPackage =
          v
          |> Toml.asTable
          |> Result.mapError Toml.TomlError.show
          |> Result.bind (tomlTableToLockedPackage packageIdentifier)

        return (packageIdentifier, lockedPackage)
      })
      |> Result.all
      |> Result.map Map.ofSeq

    let! dependencies =
      table.Rows
      |> Seq.filter (fun x -> x.Key = "dependency")
      |> Seq.map (fun x -> Toml.asTableArray x.Value |> Result.mapError Toml.TomlError.show)
      |> Result.all
      |> Result.map (Seq.collect (fun x -> x.Items))
      |> Result.map (Seq.map (tomlTableToTargetIdentifier))
      |> Result.bind (Result.all)

    return {
      ManifestHash = manifestHash;
      Dependencies = set dependencies;
      Packages = packages;
    }
  }
