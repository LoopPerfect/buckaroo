module Constraint

type Constraint = 
| Exactly of Version.Version
| Not of Constraint
| Any of List<Constraint>

let rec show ( c : Constraint) : string = 
  match c with
  | Exactly v -> Version.show v
  | Not v -> "!" + show v
  | Any xs -> 
    "[ " + 
    (xs 
      |> Seq.map (fun x -> show x) 
      |> String.concat ", ") + 
    " ]"
