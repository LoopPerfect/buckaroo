module Version

open FParsec

type Branch = string

type Revision = string

type Tag = string

type Version = 
| SemVerVersion of SemVer.SemVer
| Branch of Branch
| Revision of Revision
| Tag of Tag

let harmonious (v : Version) (u : Version) = 
  match (v, u) with 
  | (Version.SemVerVersion x, Version.SemVerVersion y) -> x = y
  | (Version.Branch x, Version.Branch y) -> x = y
  | (Version.Revision x, Version.Revision y) -> x = y
  | (Version.Tag x, Version.Tag y) -> x = y
  | _ -> true

let show (v : Version) : string = 
  match v with 
  | SemVerVersion semVer -> SemVer.show semVer
  | Branch branch -> "branch=" + branch
  | Revision revision -> "revision=" + revision
  | Tag tag -> "tag=" + tag

let identifierParser = CharParsers.regex @"[a-zA-Z\d](?:[a-zA-Z\d]|-(?=[a-zA-Z\d])){2,64}"

let tagVersionParser = parse {
  do! CharParsers.skipString "tag="
  let! tag = CharParsers.regex @"[a-zA-Z\d\\\.-_]{2,64}"
  return Tag tag
}

let branchVersionParser = parse {
  do! CharParsers.skipString "branch="
  let! tag = identifierParser
  return Branch tag
}

let revisionVersionParser = parse {
  do! CharParsers.skipString "revision="
  let! tag = identifierParser
  return Revision tag
}

let semVerVersionParser = parse {
  let! semVer = SemVer.parser
  return SemVerVersion semVer
}

let parser = 
  tagVersionParser 
  <|> branchVersionParser 
  <|> revisionVersionParser 
  <|> semVerVersionParser

let parse (x : string) : Option<Version> = 
  match run parser x with
  | Success(result, _, _) -> Some result
  | Failure(errorMsg, _, _) -> None
