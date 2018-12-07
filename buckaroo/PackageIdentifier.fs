namespace Buckaroo

type GitHubPackageIdentifier = { Owner : string; Project : string }
type GitPackageIdentifier = { Protocol : string; Uri : string }
type AdhocPackageIdentifier = { Owner : string; Project : string }

type PackageIdentifier = 
| GitHub of GitHubPackageIdentifier
| Git of GitPackageIdentifier
| Adhoc of AdhocPackageIdentifier

module PackageIdentifier = 
  
  open FParsec

  let show (id : PackageIdentifier) = 
    match id with
    | GitHub x -> "github.com/" + x.Owner + "/" + x.Project
    | Git x -> x.Protocol + x.Uri
    | Adhoc x -> x.Owner + "/" + x.Project

  let private gitHubIdentifierParser = 
    CharParsers.regex @"[a-zA-Z.\d](?:[a-zA-Z_.\d]|-(?=[a-zA-Z_.\d])){0,38}"

  let private gitUriParser = 
    CharParsers.regex @"[a-zA-Z.\d:@/-~]+"

  let private gitProtocolParser = parse {
    let! protocol = CharParsers.regex @"(ssh|file|rsync|http|https)"
    do! CharParsers.skipString "://"
    return protocol
  }
   
 

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
    do! CharParsers.skipString "github.com/" 
      <|> CharParsers.skipString "gh+"
    let! owner = gitHubIdentifierParser
    do! CharParsers.skipString "/"
    let! project = gitHubIdentifierParser
    return ({ Owner = owner; Project = project } : GitHubPackageIdentifier)
  }

  let gitPackageIdentifierParser = parse {
    let! prefix =  CharParsers.pstring "git+" <|> CharParsers.pstring "git://"
    let! protocol =
      match prefix with
      | "git://" -> parse { return "git" }
      | "git+" -> gitProtocolParser
      | _ -> parse { return "git" } // should never happen

    let! uri = gitUriParser
    return ({ Protocol = protocol; Uri = uri } : GitPackageIdentifier)
  }

  let parseGitHubIdentifier (x : string) = 
    match run (gitHubPackageIdentifierParser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error

  let parser =
    gitHubPackageIdentifierParser |>> PackageIdentifier.GitHub
    <|> (gitPackageIdentifierParser |>> PackageIdentifier.Git)
    <|> (adhocPackageIdentifierParser |>> PackageIdentifier.Adhoc)

  let parse (x : string) : Result<PackageIdentifier, string> = 
    match run (parser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error
