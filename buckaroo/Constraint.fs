namespace Buckaroo

type RangeTypes =
| MajorRange
| MinorRange
| PatchRange

type RangeComparatorTypes =
| LTE
| LT
| GT
| GTE
| DASH

type SemVerRange = {
  Min : SemVer;
  Max : SemVer;
}

type Constraint =
| Exactly of Version
| Range of SemVerRange
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

  let withinRange (r : SemVerRange) (v : SemVer) =
    r.Min <= v && r.Max > v

  let rec satisfies (c : Constraint) (v : Set<Version>) : bool =
    match c with
    | Exactly u -> v |> Set.toSeq |> Seq.exists(fun x -> x = u)
    | Complement x -> satisfies x v |> not
    | Any xs -> xs |> Seq.exists(fun c -> satisfies c v)
    | All xs -> xs |> Seq.forall(fun c -> satisfies c v)
    | Range r  ->
      v
      |> Set.toSeq
      |> Seq.tryFind (fun x ->
        match x with
        | SemVer y -> y |> withinRange r
        | _ -> false)
      |> Option.isSome


  let rec agreesWith (c : Constraint) (v : Version) : bool =
    match c with
    | Exactly u ->
      match (v, u) with
      | (Version.SemVer x, Version.SemVer y) -> x = y
      | (Version.Git(GitVersion.Branch x), Version.Git(GitVersion.Branch y)) -> x = y
      | (Version.Git(GitVersion.Revision x), Version.Git(GitVersion.Revision y)) -> x = y
      | (Version.Git(GitVersion.Tag x), Version.Git(GitVersion.Tag y)) -> x = y
      | _ -> true
    | Range r  ->
      match v with
      | SemVer v -> v |> withinRange r
      | _ -> true
    | Complement x -> agreesWith x v |> not
    | Any xs -> xs |> Seq.exists(fun c -> agreesWith c v)
    | All xs -> xs |> Seq.forall(fun c -> agreesWith c v)

  let rec compare (x : Constraint) (y : Constraint) : int =
    match (x, y) with
    | (Exactly u, Exactly v) -> Version.compare u v
    | (Range a, Range b) -> Version.compare (Version.SemVer a.Max) (Version.SemVer b.Max)
    | (Range a, Exactly b) -> Version.compare (Version.SemVer a.Max) b
    | (Exactly a, Range b) -> Version.compare a (Version.SemVer b.Max)
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
    | Range r -> r.Min.ToString() + " - " + r.Max.ToString()

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


  let symbolParser<'T> (token : string, symbol : 'T) = parse {
    do! CharParsers.skipString token
    return symbol
  }

  let rangeTypeParser = choice [
    symbolParser("^", MajorRange)
    symbolParser("~", MinorRange)
    symbolParser("+", PatchRange)
  ]

  let rangeParser = parse {
    let! rangeType = rangeTypeParser
    let! version = SemVer.parser
    return
      Range (
        match rangeType with
        | MajorRange -> {
            Min = version;
            Max = { SemVer.zero with Major = version.Major+1; }
          }
        | MinorRange -> {
            Min = version;
            Max = { SemVer.zero with Major = version.Major; Minor = version.Minor+1 }
          }
        | PatchRange -> {
            Min = version;
            Max = { version with Patch = version.Patch+1; Increment = 0 }
        }
      )
  }
  let rangeComparatorParser = choice [
    symbolParser("<=", LTE)
    symbolParser(">=", GTE)
    symbolParser("<", LT)
    symbolParser(">", GT)
    symbolParser("-", DASH)
  ]

  let customRangeParser = parse {
    let! v1 = SemVer.parser
    let! comparator = rangeComparatorParser
    let! v2 = SemVer.parser

    return
      match comparator with
      | LT -> Range {
          Min = v1;
          Max = v2;
        }
      | GT -> Range {
          Min = v2;
          Max = v1;
        }
      | LTE -> Range {
          Min = v1;
          Max = {v2 with Increment = v2.Increment+1};
        }
      | GTE -> Range {
          Min = v2;
          Max = {v1 with Increment = v1.Increment+1};
        }
      | DASH ->
        let a = if v1 < v2 then v1 else v2
        let b = if v1 >= v2 then v1 else v2
        Range {
          Min = a;
          Max = {b with Increment = b.Increment+1};
        }
  }

  let exactlyParser = parse {
    let! version = Version.parser
    return Exactly version
  }

  let Parser = parse {
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

    return! choice [
      wildcardParser
      rangeParser
      attempt(customRangeParser)
      exactlyParser
      complementParser
      anyParser
      allParser
    ]
  }
  let parse (x : string) : Result<Constraint, string> =
    match run (parser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error

