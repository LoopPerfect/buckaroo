module Buckaroo.Toml

open System

type TomlError = 
| CouldNotParse of Exception
| KeyNotFound of string
| UnexpectedType of string

module TomlError = 
  let show (x : TomlError) = 
    match x with 
    | CouldNotParse e -> "Could not parse TOML " + (string e)
    | KeyNotFound k -> "Could not find an element with key " + k
    | UnexpectedType t -> "Unexpected type " + t

let get (key : string) (table : Nett.TomlTable) = 
  match table.TryGetValue key with 
  | null -> TomlError.KeyNotFound key |> Result.Error
  | value -> Result.Ok value

let tryGet (key : string) (table : Nett.TomlTable) = 
  match table.TryGetValue key with 
  | null -> None
  | value -> Some value

let asArray (x : Nett.TomlObject) = 
  try 
    (x :?> Nett.TomlArray) |> Result.Ok
  with _ ->
    TomlError.UnexpectedType x.ReadableTypeName |> Result.Error

let asString (x : Nett.TomlObject) = 
  try 
    (x :?> Nett.TomlString).Value |> Result.Ok
  with _ ->
    TomlError.UnexpectedType x.ReadableTypeName |> Result.Error

let asBool (x : Nett.TomlObject) = 
  try 
    (x :?> Nett.TomlBool).Value |> Result.Ok
  with _ -> 
    TomlError.UnexpectedType x.ReadableTypeName |> Result.Error

let items (x : Nett.TomlArray) = 
  x.Items |> Seq.toList

let entries (x : Nett.TomlTable) = 
  x.Keys
  |> Seq.map (fun k -> (k, x.Item k))

let asTable (x : Nett.TomlObject) = 
  try 
    x :?> Nett.TomlTable |> Result.Ok
  with _ -> 
    TomlError.UnexpectedType x.ReadableTypeName |> Result.Error

let asTableArray (x : Nett.TomlObject) = 
  try 
    x :?> Nett.TomlTableArray |> Result.Ok
  with _ -> 
    TomlError.UnexpectedType x.ReadableTypeName |> Result.Error

let parse (x : string) = 
  try 
    let table = Nett.Toml.ReadString x
    table.Freeze () |> ignore
    Result.Ok table
  with error -> 
    TomlError.CouldNotParse error |> Result.Error
