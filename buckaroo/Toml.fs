module Toml

open System

let get (key : string) (table : Nett.TomlTable) : Option<Nett.TomlObject> = 
  match table.TryGetValue key with 
  | null -> None
  | value -> Some value

let asArray (x : Nett.TomlObject) : Option<Nett.TomlArray> = 
  try 
    (x :?> Nett.TomlArray) |> Some
  with | _ -> None

let asString (x : Nett.TomlObject) : Option<string> = 
  try 
    (x :?> Nett.TomlString).Value |> Some
  with | _ -> None

let items (x : Nett.TomlArray) = 
  x.Items |> Seq.toList

let asTableArray (x : Nett.TomlObject) : Option<Nett.TomlTableArray> = 
  try 
    x :?> Nett.TomlTableArray |> Some
  with | _ -> None

let parse (x : string) : Result<Nett.TomlTable, Exception> = 
  try 
    let table = Nett.Toml.ReadString x
    table.Freeze () |> ignore
    Result.Ok table
  with 
  | e -> Result.Error e
