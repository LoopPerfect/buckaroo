namespace Buckaroo

open Buckaroo.Git

type Version =
| SemVerVersion of SemVer
| Branch of Branch
| Revision of Revision
| Tag of Tag
| Latest // When we download from HTTP, version is always latest

module Version =

  open FParsec

  let compare (x : Version) (y : Version) =
    let score v =
      match v with
      | SemVerVersion _ -> 0
      | Tag _ -> 1
      | Branch _ -> 2
      | Revision _ -> 3
      | Latest _ -> 4

    match (x, y) with
    | (SemVerVersion i, SemVerVersion j) -> SemVer.compare i j
    | (Tag i, Tag j) -> System.String.Compare(i, j)
    | (Branch i, Branch j) ->
      match (i, j) with
      | ("master", "master") -> 0
      | ("master", _) -> -1
      | (_, "master") -> 1
      | ("develop", "develop") -> 0
      | ("develop", _) -> -1
      | (_, "develop") -> 1
      | (p, q) -> System.String.Compare(p, q)
    | _ -> (score x).CompareTo(score y) |> System.Math.Sign

  let compareSpecificity (x : Version) (y : Version) =
    let score v =
      match v with
      | Latest _ -> 0
      | Revision _ -> 1
      | Tag _ -> 2
      | SemVerVersion _ -> 3
      | Branch _ -> 4

    match (x, y) with
    | (Tag i, Tag j) -> System.String.Compare(i, j)
    | (SemVerVersion i, SemVerVersion j) -> SemVer.compare i j
    | (Branch i, Branch j) ->
      match (i, j) with
      | ("master", "master") -> 0
      | ("master", _) -> -1
      | (_, "master") -> 1
      | ("develop", "develop") -> 0
      | ("develop", _) -> -1
      | (_, "develop") -> 1
      | (p, q) -> System.String.Compare(p, q)
    | _ -> (score x).CompareTo(score y) |> System.Math.Sign

  let show (v : Version) : string =
    match v with
    | Latest -> "latest"
    | SemVerVersion semVer -> SemVer.show semVer
    | Branch branch -> "branch=" + branch
    | Revision revision -> "revision=" + revision
    | Tag tag -> "tag=" + tag

  let identifierParser = CharParsers.regex @"[a-zA-Z\d](?:[a-zA-Z\d]|-(?=[a-zA-Z\d])){2,64}"

  let tagVersionParser = parse {
    do! CharParsers.skipString "tag="
    let! tag = Git.branchOrTagNameParser
    return Tag tag
  }

  let branchVersionParser = parse {
    do! CharParsers.skipString "branch="
    let! branch = Git.branchOrTagNameParser
    return Branch branch
  }

  let revisionVersionParser = parse {
    do! CharParsers.skipString "revision="
    let! revision = identifierParser
    return Revision revision
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

  let parse (x : string) : Result<Version, string> =
    match run parser x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(errorMsg, _, _) -> Result.Error errorMsg
