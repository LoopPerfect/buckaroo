namespace Buckaroo

type AdhocPackageIdentifier = { Owner : string; Project : string }

type GitLabPackageIdentifier = { Groups : string list; Project : string }

type PackageIdentifier =
| GitHub of AdhocPackageIdentifier
| BitBucket of AdhocPackageIdentifier
| GitLab of GitLabPackageIdentifier
| Adhoc of AdhocPackageIdentifier

module PackageIdentifier =

  open FParsec
  open Buckaroo.RichOutput

  let show (id : PackageIdentifier) =
    match id with
    | GitHub x -> "github.com/" + x.Owner + "/" + x.Project
    | BitBucket x -> "bitbucket.org/" + x.Owner + "/" + x.Project
    | GitLab x -> "gitlab.com/" + (x.Groups |> String.concat "/") + "/" + x.Project
    | Adhoc x -> x.Owner + "/" + x.Project

  let showRich (id : PackageIdentifier) =
    id
    |> show
    |> identifier

  let private gitHubIdentifierParser =
    CharParsers.regex @"[a-zA-Z.\d](?:[a-zA-Z_.\d]|-(?=[a-zA-Z_.\d])){0,38}"

  let adhocPackageIdentifierParser = parse {
    let! owner = gitHubIdentifierParser
    do! CharParsers.skipString "/"
    let! project = gitHubIdentifierParser
    return { Owner = owner.ToLower(); Project = project.ToLower() }
  }

  let parseAdhocIdentifier (x : string) : Result<AdhocPackageIdentifier, string> =
    match run (adhocPackageIdentifierParser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error

  let gitHubPackageIdentifierParser = parse {
    do! CharParsers.skipString "github.com/" <|> CharParsers.skipString "github+"
    let! owner = gitHubIdentifierParser
    do! CharParsers.skipString "/"
    let! project = gitHubIdentifierParser
    return { Owner = owner.ToLower(); Project = project.ToLower() }
  }

  let parseGitHubIdentifier (x : string) =
    match run (gitHubPackageIdentifierParser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error

  let bitBucketPackageIdentifierParser = parse {
    do! CharParsers.skipString "bitbucket.org/" <|> CharParsers.skipString "bitbucket+"
    let! owner = gitHubIdentifierParser
    do! CharParsers.skipString "/"
    let! project = gitHubIdentifierParser
    return { Owner = owner.ToLower(); Project = project.ToLower() }
  }

  let parseBitBucketIdentifier (x : string) =
    match run (bitBucketPackageIdentifierParser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error

  let gitLabPackageIdentifierParser = parse {
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
