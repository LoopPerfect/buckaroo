namespace Buckaroo

open System
open Buckaroo
open FSharpx

type HttpLocation = {
  Url : string;
  StripPrefix : string option;
  Type : ArchiveType option;
  Sha256 : string;
}

type GitLocation = {
  Url : string;
  Revision : Revision;
}

// GitHub is different to Git because we can switch HTTPS & SSH easily
type HostedGitLocation = {
  Package : AdhocPackageIdentifier;
  Revision : Revision;
}

type PackageLocation =
| Http of HttpLocation
| Git of GitLocation
| GitHub of HostedGitLocation
| BitBucket of HostedGitLocation
| GitLab of HostedGitLocation

module PackageLocation =

  open Buckaroo.Result

  let gitHubUrl (x : AdhocPackageIdentifier) =
    if Environment.GetEnvironmentVariable "BUCKAROO_GITHUB_SSH" |> isNull
    then
      "https://github.com/" + x.Owner + "/" + x.Project + ".git"
    else
      "ssh://git@github.com:" + x.Owner + "/" + x.Project + ".git"

  let bitBucketUrl (x : AdhocPackageIdentifier) =
    if Environment.GetEnvironmentVariable "BUCKAROO_BITBUCKET_SSH" |> isNull
    then
      "https://bitbucket.org/" + x.Owner + "/" + x.Project + ".git"
    else
      "ssh://git@bitbucket.org:" + x.Owner + "/" + x.Project + ".git"

  let gitLabUrl (x : AdhocPackageIdentifier) =
    if Environment.GetEnvironmentVariable "BUCKAROO_GITLAB_SSH" |> isNull
    then
      "https://gitlab.com/" + x.Owner + "/" + x.Project + ".git"
    else
      "ssh://git@gitlab.com:" + x.Owner + "/" + x.Project + ".git"
  let show (x : PackageLocation) =
    match x with
    | Git g -> g.Url + "#" + g.Revision
    | Http y ->
      y.Url +
      (y.StripPrefix |> Option.map ((+) "#") |> Option.defaultValue "") +
      (y.Type |> Option.map (ArchiveType.show) |> Option.map (fun x -> "#" + x) |> Option.defaultValue "")
    | GitHub y -> (gitHubUrl y.Package) + "#" + y.Revision
    | BitBucket y -> (bitBucketUrl y.Package) + "#" + y.Revision
    | GitLab y -> (gitLabUrl y.Package) + "#" + y.Revision

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

      return PackageLocation.Git { Url = git; Revision = revision }
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
          return PackageLocation.GitHub { Package = x; Revision = revision }
        | PackageIdentifier.BitBucket x -> 
          return PackageLocation.BitBucket { Package = x; Revision = revision }
        | PackageIdentifier.GitLab x -> 
          return PackageLocation.GitLab { Package = x; Revision = revision }
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
         PackageLocation.Http 
         { Url = url; Sha256 = sha256; StripPrefix = stripPrefix; Type = archiveType }
  }