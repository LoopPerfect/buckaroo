namespace Buckaroo

type ArchiveType = | Zip

module ArchiveType = 

  type ParseError = 
  | InvalidType of string

  let parse (x : string) = 
    match x.Trim().ToLower() with 
    | "zip" -> Ok ArchiveType.Zip
    | _ -> x |> ParseError.InvalidType |> Error

  let show x = 
    match x with 
    | Zip -> "zip"
