namespace Buckaroo

type GitHubPackageIdentifier = { Owner : string; Project : string }

type AdhocPackageIdentifier = { Owner : string; Project : string }

type PackageIdentifier = 
| GitHub of GitHubPackageIdentifier
| Adhoc of AdhocPackageIdentifier

module PackageIdentifier = 
  
  open FParsec

  let show (id : PackageIdentifier) = 
    match id with
    | GitHub x -> x.Owner + "/" + x.Project
    | Adhoc x -> x.Owner + "/" + x.Project

  let gitHubIdentifierParser = CharParsers.regex @"[a-zA-Z.\d](?:[a-zA-Z.\d]|-(?=[a-zA-Z.\d])){0,38}"

  let gitHubPackageIdentifierParser = parse {
    do! CharParsers.skipString "github.com/" <|> CharParsers.skipString "github+"
    let! owner = gitHubIdentifierParser
    do! CharParsers.skipString "/"
    let! project = gitHubIdentifierParser
    return GitHub { Owner = owner; Project = project }
  }

  let parser = gitHubPackageIdentifierParser

  let parse (x : string) : Result<PackageIdentifier, string> = 
    match run (parser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error
