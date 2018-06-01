module Constraint

type Constraint = 
| Exactly of Version.Version
| Not of Constraint
| Any of List<Constraint>
| All of List<Constraint>

let intersection (c : Constraint) (d : Constraint) : Constraint = 
  All [ c; d ]

let union (c : Constraint) (d : Constraint) : Constraint = 
  Any [ c; d ]

let complement (c : Constraint) : Constraint = 
  Not c

let rec satisfies (c : Constraint) (v : Version.Version) : bool = 
  match c with
  | Exactly x -> v = x 
  | Not c -> satisfies c v |> not 
  | Any xs -> xs |> Seq.exists(fun c -> satisfies c v)
  | All xs -> xs |> Seq.forall(fun c -> satisfies c v)

let rec show ( c : Constraint) : string = 
  match c with
  | Exactly v -> Version.show v
  | Not v -> "!" + show v
  | Any xs -> 
    "any(" + 
    (xs 
      |> Seq.map (fun x -> show x) 
      |> String.concat ", ") + 
    ")"
  | All xs -> 
    "all(" + 
    (xs 
      |> Seq.map (fun x -> show x) 
      |> String.concat ", ") + 
    ")"
