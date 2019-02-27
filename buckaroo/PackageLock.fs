namespace Buckaroo

open System
open Buckaroo
open FSharpx

type PackageLock =
| Http of HttpLocation * string
| Git of GitLocation
| GitHub of HostedGitLocation
| BitBucket of HostedGitLocation
| GitLab of GitLabLocation

module PackageLock =

  open Buckaroo.Result

  let toLocation (x : PackageLock) : PackageLocation =
    match x with
    | Http (location, sha256) -> PackageLocation.Http location
    | Git git -> PackageLocation.Git git
    | GitHub git -> PackageLocation.GitHub git
    | BitBucket git -> PackageLocation.BitBucket git
    | GitLab git -> PackageLocation.GitLab git

  let show (x : PackageLock) =
    match x with
    | Git g -> g.Url + "#" + g.Revision
    | Http (y, _) ->
      y.Url +
      (y.StripPrefix |> Option.map ((+) "#") |> Option.defaultValue "") +
      (y.Type |> Option.map (ArchiveType.show) |> Option.map (fun x -> "#" + x) |> Option.defaultValue "")
    | GitHub y -> (PackageLocation.gitHubUrl y.Package) + "#" + y.Revision
    | BitBucket y -> (PackageLocation.bitBucketUrl y.Package) + "#" + y.Revision
    | GitLab y -> (PackageLocation.gitLabUrl y.Package) + "#" + y.Revision

  let fromToml toml = result {
    match toml |> Toml.tryGet "git" with
    | Some gitToml ->
      let! git =
        gitToml
        |> Toml.asString
        |> Result.mapError Toml.TomlError.show

      let! revision =
        toml
        |> Toml.get "revision"
        |> Result.bind Toml.asString
        |> Result.mapError Toml.TomlError.show

      return PackageLock.Git { Url = git; Revision = revision }
    | None ->
      match toml |> Toml.tryGet "package" with
      | Some tomlPackage ->
        let! package =
          tomlPackage
          |> Toml.asString
          |> Result.mapError Toml.TomlError.show
          |> Result.bind PackageIdentifier.parse

        let! revision =
          toml
          |> Toml.get "revision"
          |> Result.bind Toml.asString
          |> Result.mapError Toml.TomlError.show

        match package with
        | PackageIdentifier.Adhoc _ ->
          return! Result.Error "Expected a hosted-git package"
        | PackageIdentifier.GitHub x ->
          return PackageLock.GitHub { Package = x; Revision = revision }
        | PackageIdentifier.BitBucket x ->
          return PackageLock.BitBucket { Package = x; Revision = revision }
        | PackageIdentifier.GitLab x ->
          return PackageLock.GitLab { Package = x; Revision = revision }
      | None ->
        let! url =
          toml
          |> Toml.get "url"
          |> Result.bind Toml.asString
          |> Result.mapError Toml.TomlError.show

        let! sha256 =
          toml
          |> Toml.get "sha256"
          |> Result.bind Toml.asString
          |> Result.mapError Toml.TomlError.show

        let! stripPrefix =
          toml
          |> Toml.tryGet "strip_prefix"
          |> Option.map (Toml.asString)
          |> Option.map (Result.map Option.Some)
          |> Option.map (Result.mapError Toml.TomlError.show)
          |> Option.defaultValue (Result.Ok Option.None)

        let! archiveType =
          toml
          |> Toml.tryGet "type"
          |> Option.map (Toml.asString)
          |> Option.map (Result.mapError Toml.TomlError.show)
          |> Option.map (Result.bind (ArchiveType.parse >> Result.mapError ArchiveType.ParseError.show))
          |> Option.map (Result.map Option.Some)
          |> Option.defaultValue (Result.Ok Option.None)

        return
          PackageLock.Http
          ({ Url = url; StripPrefix = stripPrefix; Type = archiveType }, sha256)
  }