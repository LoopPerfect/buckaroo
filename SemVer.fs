module SemVer

open System

type SemVer = { Major : int; Minor : int; Patch : int; Increment : int }

let zero : SemVer = { Major = 0; Minor = 0; Patch = 0; Increment = 0 }

let compare (x : SemVer) (y : SemVer) = 
  match x.Major.CompareTo y.Major with
  | 0 -> 
    match x.Minor.CompareTo y.Minor with
    | 0 -> 
      match x.Patch.CompareTo y.Patch with
      | 0 -> x.Increment.CompareTo y.Increment
      | c -> c
    | c -> c
  | c -> c

let show (x : SemVer) : string = 
  let elements = 
    if x.Increment = 0 
    then [ x.Major; x.Minor; x.Patch ] 
    else [ x.Major; x.Minor; x.Patch; x.Increment ] 
  elements
    |> Seq.map (fun x -> string x)
    |> String.concat "."

let parseInt (x : string) = 
  try
      let i = System.Int32.Parse x
      Some i
  with _ -> None

let parse (x : string) : Option<SemVer> = 
  let y = 
    let trimmed = x.Trim()
    if trimmed.StartsWith("v", StringComparison.OrdinalIgnoreCase)
    then trimmed.Substring 1
    else trimmed
  let parts = y.Split [|'.'|]
  match parts.Length with 
  | x when x = 3 || x = 4 -> 
    let step state next = 
      match (state, next) with
      | (Some xs, Some x) -> [ x ] |> List.append xs |> Some
      | _ -> None
    let intParts = 
      parts 
        |> Seq.map parseInt
        |> Seq.fold step (Some [])
    match intParts with
    | Some xs -> 
      { 
        zero with 
          Major = xs.[0]; 
          Minor = xs.[1]; 
          Patch = xs.[2]; 
          Increment = try xs.[3] with | _ -> 0 } |> Some
    | None -> None
  | _ -> None
