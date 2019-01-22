namespace Buckaroo

/// A target inside of a Buck cell
/// For example:
///  * "//:lib"
///  * ":lib"
///  * "//path/to/some:lib"
///  * "//path/to/some"
type Target = {
  Folders : string list;
  Name : string;
}

module Target =

  open FParsec

  let show (x : Target) : string =
    "//" + (x.Folders |> String.concat "/") + ":" + x.Name

  let segmentParser = CharParsers.regex @"[a-zA-Z.\d](?:[a-zA-Z.\d]|_|\+|-(?=[a-zA-Z.\d])){0,38}"

  let explicitNameParser = parse {
    let slash = CharParsers.skipString "/"
    let! folders = Primitives.sepBy segmentParser slash
    do! slash |> Primitives.optional
    do! CharParsers.skipString ":"
    let! name = segmentParser
    return { Folders = folders; Name = name }
  }

  let implicitNameParser = parse {
    let slash = CharParsers.skipString "/"
    let! folders = Primitives.sepBy1 segmentParser slash
    do! slash |> Primitives.optional
    let name = folders |> List.rev |> List.head
    return { Folders = folders; Name = name }
  }

  let parser = parse {
    do! CharParsers.skipString "//" |> Primitives.optional
    return! Primitives.choice [ Primitives.attempt explicitNameParser; implicitNameParser ]
  }

  let parse (x : string) : Result<Target, string> =
    match run (parser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error
