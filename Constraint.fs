module Constraint

open FParsec

type Version = Version.Version

type Constraint = 
| Exactly of Version
| Complement of Constraint
| Any of List<Constraint>
| All of List<Constraint>

let wildcard = All []

let intersection (c : Constraint) (d : Constraint) : Constraint = 
  All [ c; d ]

let union (c : Constraint) (d : Constraint) : Constraint = 
  Any [ c; d ]

let complement (c : Constraint) : Constraint = 
  Complement c

let rec satisfies (c : Constraint) (v : Version) : bool = 
  match c with
  | Exactly u -> v = u
  | Complement x -> satisfies x v |> not 
  | Any xs -> xs |> Seq.exists(fun c -> satisfies c v)
  | All xs -> xs |> Seq.forall(fun c -> satisfies c v)

let rec agreesWith (c : Constraint) (v : Version) : bool = 
  match c with
  | Exactly u -> 
    match (v, u) with 
    | (Version.SemVerVersion x, Version.SemVerVersion y) -> x = y
    | (Version.Branch x, Version.Branch y) -> x = y
    | (Version.Revision x, Version.Revision y) -> x = y
    | (Version.Tag x, Version.Tag y) -> x = y
    | _ -> true
  | Complement x -> agreesWith x v |> not 
  | Any xs -> xs |> Seq.exists(fun c -> agreesWith c v)
  | All xs -> xs |> Seq.forall(fun c -> agreesWith c v)

let rec show ( c : Constraint) : string = 
  match c with
  | Exactly v -> Version.show v
  | Complement c -> "!" + show c
  | Any xs -> 
    "any(" + 
    (xs 
      |> Seq.map (fun x -> show x) 
      |> String.concat ", ") + 
    ")"
  | All xs -> 
    if Seq.isEmpty xs 
    then "*"
    else 
      "all(" + 
      (xs 
        |> Seq.map (fun x -> show x) 
        |> String.concat ", ") + 
      ")"

let wildcardParser = parse {
  do! CharParsers.skipString "*"
  return All []
}

let exactlyParser = parse {
  let! version = Version.parser
  return Exactly version
}

#nowarn "40"
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

let parse (x : string) : Option<Constraint> = 
  match run parser x with
  | Success(result, _, _) -> Some result
  | Failure(errorMsg, _, _) -> None
