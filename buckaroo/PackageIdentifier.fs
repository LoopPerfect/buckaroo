namespace Buckaroo

type AdhocPackageIdentifier = { Owner : string; Project : string }

type PackageIdentifier = 
| GitHub of AdhocPackageIdentifier
| BitBucket of AdhocPackageIdentifier
| Adhoc of AdhocPackageIdentifier

module PackageIdentifier = 
  
  open FParsec

  let show (id : PackageIdentifier) = 
    match id with
    | GitHub x -> "github.com/" + x.Owner + "/" + x.Project
    | BitBucket x -> "bitbucket.org/" + x.Owner + "/" + x.Project
    | Adhoc x -> x.Owner + "/" + x.Project

  let private gitHubIdentifierParser = 
    CharParsers.regex @"[a-zA-Z.\d](?:[a-zA-Z_.\d]|-(?=[a-zA-Z_.\d])){0,38}"

  let adhocPackageIdentifierParser = parse {
    let! owner = gitHubIdentifierParser
    do! CharParsers.skipString "/"
    let! project = gitHubIdentifierParser
    return { Owner = owner; Project = project }
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
    return { Owner = owner; Project = project }
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
    return { Owner = owner; Project = project }
  }

  let parseBitBucketIdentifier (x : string) = 
    match run (bitBucketPackageIdentifierParser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error

  let parser =
    gitHubPackageIdentifierParser |>> PackageIdentifier.GitHub
    <|> (bitBucketPackageIdentifierParser |>> PackageIdentifier.BitBucket)
    <|> (adhocPackageIdentifierParser |>> PackageIdentifier.Adhoc)

  let parse (x : string) : Result<PackageIdentifier, string> = 
    match run (parser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error
