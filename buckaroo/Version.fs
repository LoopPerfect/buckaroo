namespace Buckaroo

open Buckaroo.Git

type GitVersion =
| Revision of Revision
| Branch of Branch
| Tag of Tag

type Version =
| Git of GitVersion
| SemVer of SemVer


module Version =

  open FParsec

  // score returns ranks version by the likelyhood of change.
  let private score v =
    match v with
    | Git (Revision _) -> 0
    | Git (Tag _) -> 1
    | SemVer _ -> 2
    | Git (Branch "master") -> 4
    | Git (Branch "develop") -> 5
    | Git (Branch _) -> 3

  let compare (x : Version) (y : Version) =
    match (score x - score y, x, y) with
    | (0, Version.SemVer i, Version.SemVer j) -> SemVer.compare i j
    | (0, Git(Tag i), Git(Tag j)) -> System.String.Compare(i, j)
    | (0, Git(Branch i), Git(Branch j)) -> System.String.Compare(i, j)
    | (0, Git(Revision i), Git(Revision j)) -> System.String.Compare(i, j)
    | (c, _, _) -> c

  let rec show (v : Version) : string =
    match v with
    | SemVer semVer -> SemVer.show semVer
    | Git(Branch branch) -> "branch=" + branch
    | Git(Revision revision) -> "revision=" + revision
    | Git(Tag tag) -> "tag=" + tag

  let identifierParser = CharParsers.regex @"[a-zA-Z\d](?:[a-zA-Z\d]|-(?=[a-zA-Z\d])){2,64}"

  let tagVersionParser = parse {
    do! CharParsers.skipString "tag="
    let! tag = Git.branchOrTagNameParser
    return Version.Git (GitVersion.Tag tag)
  }

  let branchVersionParser = parse {
    do! CharParsers.skipString "branch="
    let! branch = Git.branchOrTagNameParser
    return Version.Git (GitVersion.Branch branch)
  }

  let revisionVersionParser = parse {
    do! CharParsers.skipString "revision="
    let! revision = identifierParser
    return Version.Git (GitVersion.Revision revision)
  }

  let semVerVersionParser = parse {
    let! semVer = SemVer.parser
    return Version.SemVer semVer
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
