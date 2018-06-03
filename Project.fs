module Project

open FParsec

type GitHubProject = { Owner : string; Project : string }

type Project = 
| GitHub of GitHubProject

let sourceLocation (p : Project) = 
  match p with
  | GitHub x -> "https://github.com/" + x.Owner + "/" + x.Project + ".git"

let gitHubIdentifierParser = CharParsers.regex @"[a-zA-Z\d](?:[a-zA-Z\d]|-(?=[a-zA-Z\d])){0,38}"

let gitHubProjectParser = parse {
  do! CharParsers.skipString "github.com/" <|> CharParsers.skipString "github+"
  let! owner = gitHubIdentifierParser
  do! CharParsers.skipString "/"
  let! project = gitHubIdentifierParser
  return GitHub { Owner = owner; Project = project }
}

let parser = gitHubProjectParser

let parse (x : string) : Option<Project> = 
  match run parser x with
  | Success(result, _, _) -> Some result
  | Failure(errorMsg, _, _) -> None

let show (p : Project) : string = 
  match p with
  | GitHub x -> "github.com/" + x.Owner + "/" + x.Project
