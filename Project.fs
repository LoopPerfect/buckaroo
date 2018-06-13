module Project

open System.IO
open FParsec

type GitHubProject = { Owner : string; Project : string }

type Project = 
| GitHub of GitHubProject

let defaultTarget (project : Project) = 
  match project with
  | GitHub x -> "//:" + x.Project

let cellName (project : Project) = 
  match project with
  | GitHub x -> 
    [ "github"; x.Owner.ToLower(); x.Project.ToLower() ] 
    |> String.concat "-"

let sourceLocation (p : Project) = 
  match p with
  // | GitHub x -> "ssh://git@github.com:" + x.Owner + "/" + x.Project + ".git"
  | GitHub x -> "https://github.com/" + x.Owner + "/" + x.Project + ".git"

let installSubPath (p : Project) = 
  match p with
  | GitHub x -> Path.Combine("github", x.Owner.ToLower(), x.Project.ToLower())

let gitHubIdentifierParser = CharParsers.regex @"[a-zA-Z.\d](?:[a-zA-Z.\d]|-(?=[a-zA-Z.\d])){0,38}"

let gitHubProjectParser = parse {
  do! CharParsers.skipString "github.com/" <|> CharParsers.skipString "github+"
  let! owner = gitHubIdentifierParser
  do! CharParsers.skipString "/"
  let! project = gitHubIdentifierParser
  return GitHub { Owner = owner; Project = project }
}

let parser = gitHubProjectParser

let parse (x : string) : Result<Project, string> = 
  match run (parser .>> CharParsers.eof) x with
  | Success(result, _, _) -> Result.Ok result
  | Failure(error, _, _) -> Result.Error error

let show (p : Project) : string = 
  match p with
  | GitHub x -> "github.com/" + x.Owner + "/" + x.Project
