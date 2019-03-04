namespace Buckaroo

type RangeType =
| Major // ^1.2.3 in NPM
| Minor // ~1.2.3 in NPM
| Patch // Our own invention!

type RangeComparatorTypes =
| LTE
| LT
| GT
| GTE
  with
    override this.ToString () =
      match this with
      | LTE -> "<="
      | LT -> "<"
      | GT -> ">"
      | GTE -> ">="

type Constraint =
| Exactly of Version
| Range of RangeComparatorTypes * SemVer
| Any of Set<Constraint>
| All of Set<Constraint>
| Complement of Constraint

#nowarn "40"

module Constraint =

  open FParsec

  let wildcard = All Set.empty

  let intersection (c : Constraint) (d : Constraint) : Constraint =
    All (Set[ c; d ])

  let union (c : Constraint) (d : Constraint) : Constraint =
    Any (Set[ c; d ])

  let complement (c : Constraint) : Constraint =
    Complement c

  let isWithinRange (c, v) (candidate : SemVer) =
    match c with
    | LTE ->
      candidate <= v
    | LT ->
      candidate < v
    | GT ->
      candidate > v
    | GTE ->
      candidate >= v

  let rec satisfies (c : Constraint) (vs : Set<Version>) : bool =
    match c with
    | Exactly u -> vs |> Set.toSeq |> Seq.exists(fun x -> x = u)
    | Complement x -> satisfies x vs |> not
    | Any xs -> xs |> Seq.exists (fun c -> satisfies c vs)
    | All xs -> xs |> Seq.forall (fun c -> satisfies c vs)
    | Range (op, v) ->
      vs
      |> Set.toSeq
      |> Seq.exists (fun x ->
        match x with
        | SemVer semVer -> semVer |> isWithinRange (op, v)
        | _ -> false
      )
  let rec compare (x : Constraint) (y : Constraint) : int =
    match (x, y) with
    | (Exactly u, Exactly v) -> Version.compare u v
    | (Range (_, u), Range (_, v)) ->
      Version.compare (Version.SemVer u) (Version.SemVer v)
    | (Range (_, v), Exactly b) ->
      Version.compare (Version.SemVer v) b
    | (Exactly a, Range (_, v)) ->
      Version.compare a (Version.SemVer v)
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

  let rec show (c : Constraint) : string =
    match c with
    | Exactly v -> Version.show v
    | Complement c -> "!" + show c
    | Any xs ->
      "any(" +
      (xs
        |> Seq.map show
        |> String.concat " ") +
      ")"
    | All xs ->
      if Seq.isEmpty xs
      then "*"
      else
        "all(" +
        (xs
          |> Seq.map show
          |> String.concat " ") +
        ")"
    | Range (op, v) -> (string op) + (string v)

  let rec simplify (c : Constraint) : Constraint =
    let iterate c =
      match c with
      | Complement (Complement x) -> x
      | Constraint.All xs ->
        match xs |> Set.toList with
        | [x] -> x
        | xs ->
          xs
          |> Seq.collect (fun x ->
            match x with
            | All xs -> xs
            | _ -> Set[ x ]
          )
          |> Seq.sort
          |> Seq.distinct
          |> Set
          |> Constraint.All
      | Constraint.Any xs ->
        match xs |> Set.toList with
        | [x] -> x
        | xs ->
          xs
          |> Seq.collect (fun x ->
            match x with
            | Any xs -> xs
            | _ -> Set[ x ]
          )
          |> Seq.sort
          |> Seq.distinct
          |> Set
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
    return All Set.empty
  }

  let symbolParser<'T> (token : string, symbol : 'T) = parse {
    do! CharParsers.skipString token
    return symbol
  }

  let rangeTypeParser = choice [
    symbolParser ("^", Major)
    symbolParser ("~", Minor)
    symbolParser ("+", Patch)
  ]

  let rangeToConstraint rangeType semVer =
    let max =
      match rangeType with
      | Major ->
        { SemVer.zero with Major = semVer.Major + 1; }
      | Minor ->
        { SemVer.zero with Major = semVer.Major; Minor = semVer.Minor + 1 }
      | Patch ->
        { semVer with Patch = semVer.Patch + 1; Increment = 0 }
    Constraint.All
      (Set[
        Constraint.Range (GTE, semVer);
        Constraint.Range (LT, max);
      ])

  let rangeParser = parse {
    let! rangeType = rangeTypeParser
    let! semVer = SemVer.parser

    return rangeToConstraint rangeType semVer
  }

  let rangeComparatorParser = choice [
    symbolParser ("<=", LTE)
    symbolParser (">=", GTE)
    symbolParser ("<", LT)
    symbolParser (">", GT)
  ]

  let customRangeParser = parse {
    let! comparator = rangeComparatorParser
    let! semVer = SemVer.parser

    return Constraint.Range (comparator, semVer)
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

      return Any (Set elements)
    }

    let allParser = parse {
      do! CharParsers.skipString "all("
      let! elements = CharParsers.spaces1 |> Primitives.sepBy parser
      do! CharParsers.skipString ")"

      return All (Set elements)
    }

    return! choice [
      wildcardParser
      rangeParser
      attempt (customRangeParser)
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

