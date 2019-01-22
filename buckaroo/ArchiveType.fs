namespace Buckaroo

type ArchiveType =
| Zip

module ArchiveType =

  type ParseError =
  | InvalidType of string

  module ParseError =
    let show (x : ParseError) =
      match x with
      | ParseError.InvalidType s -> s

  let parse (x : string) =
    match x.Trim().ToLower() with
    | "zip" -> Ok ArchiveType.Zip
    | _ -> x |> ParseError.InvalidType |> Error

  let show x =
    match x with
    | Zip -> "zip"
