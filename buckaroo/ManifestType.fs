namespace Buckaroo

open FParsec

type ManifestType =
| BUCKAROO_TOML
| OPTIONAL_BUCKAROO_TOML
| NO_MANIFEST
| NPMJS
| MAVEN


module ManifestType =

  let toString manifestType =
    match manifestType with
    | BUCKAROO_TOML -> "bpm"
    | OPTIONAL_BUCKAROO_TOML -> "bpm?"
    | NO_MANIFEST -> "raw"
    | NPMJS -> "npmjs"
    | MAVEN -> "mvn"

  let fromString str =
    match str with
    | "" -> Result.Ok BUCKAROO_TOML
    | "bpm" ->  Result.Ok BUCKAROO_TOML
    | "bpm?+" -> Result.Ok OPTIONAL_BUCKAROO_TOML
    | "raw" -> Result.Ok NO_MANIFEST
    | "npmjs" -> Result.Ok NPMJS
    | "mvn" -> Result.Ok MAVEN
    | x -> Result.Error ("ManifestType '" + str + "' not supported")

  let fromToml toml =
    toml
    |> Toml.tryGet "manifestType"
    |> Option.map Toml.asString
    |> Option.defaultValue (Result.Ok "")
    |> Result.mapError Toml.TomlError.show
    |> Result.bind fromString

  let show manifestType =
    match manifestType with
    | BUCKAROO_TOML -> "bpm+"
    | OPTIONAL_BUCKAROO_TOML -> "bpm?+"
    | NO_MANIFEST -> "raw+"
    | NPMJS -> "npmjs+"
    | MAVEN -> "mvn+"

  let parseManifestType = parse {
    return!
      (skipString "bpm+" >>% ManifestType.BUCKAROO_TOML)
      <|> (skipString "npmjs+" >>% ManifestType.BUCKAROO_TOML)
      <|> (skipString "mvn+" >>% ManifestType.MAVEN)
      <|> (skipString "raw+" >>% ManifestType.NO_MANIFEST)
      <|> (skipString "" >>% ManifestType.BUCKAROO_TOML)
    }

  let parser = parse {
    let! manifestType = parseManifestType
    return manifestType
  }

  let parse (x : string) : Result<ManifestType, string> =
    match run (parser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error
