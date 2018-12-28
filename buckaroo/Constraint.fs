namespace Buckaroo
open System.Data
open System.Data
open System.Data

type Constraint =
| Exactly of Version
| Any of List<Constraint>
| All of List<Constraint>
| Complement of Constraint

#nowarn "40"

module Constraint =

  open FParsec

  let wildcard = All []

  let intersection (c : Constraint) (d : Constraint) : Constraint =
    All [ c; d ]

  let union (c : Constraint) (d : Constraint) : Constraint =
    Any [ c; d ]

  let complement (c : Constraint) : Constraint =
    Complement c

  let rec satisfies (c : Constraint) (v : Set<Version>) : bool =
    match c with
    | Exactly u -> v |> Set.toSeq |> Seq.exists(fun x -> x = u)
    | Complement x -> satisfies x v |> not
    | Any xs -> xs |> Seq.exists(fun c -> satisfies c v)
    | All xs -> xs |> Seq.forall(fun c -> satisfies c v)

  let rec agreesWith (c : Constraint) (v : Version) : bool =
    match c with
    | Exactly u ->
      match (v, u) with
      | (Version.SemVer x, Version.SemVer y) -> x = y
      | (Version.Git(GitVersion.Branch x), Version.Git(GitVersion.Branch y)) -> x = y
      | (Version.Git(GitVersion.Revision x), Version.Git(GitVersion.Revision y)) -> x = y
      | (Version.Git(GitVersion.Tag x), Version.Git(GitVersion.Tag y)) -> x = y
      | _ -> true
    | Complement x -> agreesWith x v |> not
    | Any xs -> xs |> Seq.exists(fun c -> agreesWith c v)
    | All xs -> xs |> Seq.forall(fun c -> agreesWith c v)

  let rec compare (x : Constraint) (y : Constraint) : int =
    match (x, y) with
    | (Exactly u, Exactly v) -> Version.compare u v
    | (Any xs, y) ->
      xs
      |> Seq.map (fun x -> compare x y)
      |> Seq.append [ -1 ]
      |> Seq.max
    | (y, Any xs) ->
      (compare (Any xs) y) * -1
    | (All xs, y) ->
      xs
      |> Seq.map (fun x -> compare x y)
      |> Seq.append [ 1 ]
      |> Seq.min
    | (y, All xs) ->
      (compare (All xs) y) * -1
    | (Complement c, y) ->
      compare y c
    | (y, Complement c) ->
      compare c y

  let rec show ( c : Constraint) : string =
    match c with
    | Exactly v -> Version.show v
    | Complement c -> "!" + show c
    | Any xs ->
      "any(" +
      (xs
        |> Seq.map (fun x -> show x)
        |> String.concat " ") +
      ")"
    | All xs ->
      if Seq.isEmpty xs
      then "*"
      else
        "all(" +
        (xs
          |> Seq.map (fun x -> show x)
          |> String.concat " ") +
        ")"

  let rec simplify (c : Constraint) : Constraint =
    let iterate c =
      match c with
      | Complement (Complement x) -> x
      | Constraint.All [ x ] -> x
      | Constraint.All xs ->
        xs
        |> Seq.collect (fun x ->
          match x with
          | All xs -> xs
          | _ -> [ x ]
        )
        |> Seq.sort
        |> Seq.distinct
        |> Seq.toList
        |> Constraint.All
      | Constraint.Any [ x ] -> x
      | Constraint.Any xs ->
        xs
        |> Seq.collect (fun x ->
          match x with
          | Any xs -> xs
          | _ -> [ x ]
        )
        |> Seq.sort
        |> Seq.distinct
        |> Seq.toList
        |> Constraint.Any
      | _ -> c
    let next = iterate c
    if next = c
    then
      c
    else
      simplify next

  let wildcardParser = parse {
    do! CharParsers.skipString "*"
    return All []
  }

  let exactlyParser = parse {
    let! version = Version.parser
    return Exactly version
  }

  let rec parser = parse {
    let complementParser = parse {
      do! CharParsers.skipString "!"
      let! c = parser
      return Complement c
    }

    let anyParser = parse {
      do! CharParsers.skipString "any("
      let! elements = CharParsers.spaces1 |> Primitives.sepBy parser
      do! CharParsers.skipString ")"
      return Any elements
    }

    let allParser = parse {
      do! CharParsers.skipString "all("
      let! elements = CharParsers.spaces1 |> Primitives.sepBy parser
      do! CharParsers.skipString ")"
      return All elements
    }

    return!
      wildcardParser
      <|> exactlyParser
      <|> complementParser
      <|> anyParser
      <|> allParser
  }

  let parse (x : string) : Result<Constraint, string> =
    match run (parser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error

