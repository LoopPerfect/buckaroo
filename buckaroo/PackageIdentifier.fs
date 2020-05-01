namespace Buckaroo

type AdhocPackageIdentifier = { Owner : string; Project : string; Type : ManifestType }

type GitLabPackageIdentifier = { Groups : string list; Project : string; Type : ManifestType }

type PackageIdentifier =
| GitHub of AdhocPackageIdentifier
| BitBucket of AdhocPackageIdentifier
| GitLab of GitLabPackageIdentifier
| Adhoc of AdhocPackageIdentifier

module PackageIdentifier =

  open FParsec
  open Buckaroo.RichOutput

  let getManifestType (id : PackageIdentifier) =
    match id with
    | GitHub x -> x.Type
    | BitBucket x -> x.Type
    | GitLab x -> x.Type
    | Adhoc x -> x.Type

  let show (id : PackageIdentifier) =
    match id with
    | GitHub x ->
      ManifestType.show x.Type + "github.com/" + x.Owner + "/" + x.Project
    | BitBucket x ->
      ManifestType.show x.Type + "bitbucket.org/" + x.Owner + "/" + x.Project
    | GitLab x ->
      ManifestType.show x.Type + "gitlab.com/" + (x.Groups |> String.concat "/") + "/" + x.Project
    | Adhoc x ->
      ManifestType.show x.Type + x.Owner + "/" + x.Project

  let showRich (id : PackageIdentifier) =
    id
    |> show
    |> identifier

  let private gitHubIdentifierParser =
    CharParsers.regex @"[a-zA-Z.\d](?:[a-zA-Z_.\d]|-(?=[a-zA-Z_.\d])){0,38}"

  let adhocPackageIdentifierParser = parse {
    let! manifestType = ManifestType.parser
    let! owner = gitHubIdentifierParser
    do! CharParsers.skipString "/"
    let! project = gitHubIdentifierParser
    return { Owner = owner.ToLower(); Project = project.ToLower(); Type = manifestType }
  }

  let parseAdhocIdentifier (x : string) : Result<AdhocPackageIdentifier, string> =
    match run (adhocPackageIdentifierParser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error

  let gitHubPackageIdentifierParser = parse {
    let! manifestType = ManifestType.parser
    do! CharParsers.skipString "github.com/" <|> CharParsers.skipString "github+"
    let! owner = gitHubIdentifierParser
    do! CharParsers.skipString "/"
    let! project = gitHubIdentifierParser
    return { Owner = owner.ToLower(); Project = project.ToLower(); Type = manifestType  }
  }

  let parseGitHubIdentifier (x : string) =
    match run (gitHubPackageIdentifierParser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error

  let bitBucketPackageIdentifierParser = parse {
    let! manifestType = ManifestType.parser
    do! CharParsers.skipString "bitbucket.org/" <|> CharParsers.skipString "bitbucket+"
    let! owner = gitHubIdentifierParser
    do! CharParsers.skipString "/"
    let! project = gitHubIdentifierParser
    return { Owner = owner.ToLower(); Project = project.ToLower(); Type = manifestType  }
  }

  let parseBitBucketIdentifier (x : string) =
    match run (bitBucketPackageIdentifierParser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error

  let gitLabPackageIdentifierParser = parse {
    let! manifestType = ManifestType.parser
    do! CharParsers.skipString "gitlab.com/"
    let! x = gitHubIdentifierParser
    do! CharParsers.skipString "/"
    let! xs = Primitives.sepBy1 gitHubIdentifierParser (skipString "/")
    let parts =
      [ x ] @ xs
      |> List.map (fun x -> x.ToLower ())

    return {
      Groups = parts |> List.truncate (parts.Length - 1)
      Project = parts |> List.last
      Type = manifestType
    }
  }

  let parseGitLabIdentifier (x : string) =
    match run (gitLabPackageIdentifierParser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error

  let parser =
    gitHubPackageIdentifierParser |>> PackageIdentifier.GitHub
    <|> (bitBucketPackageIdentifierParser |>> PackageIdentifier.BitBucket)
    <|> (gitLabPackageIdentifierParser |>> PackageIdentifier.GitLab)
    <|> (adhocPackageIdentifierParser |>> PackageIdentifier.Adhoc)

  let parse (x : string) : Result<PackageIdentifier, string> =
    match run (parser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error
