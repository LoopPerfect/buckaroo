// // based on 
// // https://github.com/mackwic/To.ml
// // https://github.com/seliopou/toml

module Toml

open System
open System.Globalization

let asString (x : Nett.TomlObject) : Option<string> = 
  try 
    (x :?> Nett.TomlString).Value |> Some
  with | _ -> None

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
