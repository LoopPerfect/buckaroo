namespace Buckaroo

open Buckaroo.BuckConfig
open Buckaroo.Result
open Buckaroo.Toml


type FeatureUnitValue =
| Boolean of bool
| Integer of int64
| String of string
// | Version of Version

type FeatureValue =
| Value of FeatureUnitValue
| Dictionary of Map<string, FeatureUnitValue>
| Array of List<FeatureUnitValue>


module FeatureUnitValue =
  let show (x : FeatureUnitValue) : string =
    match x with
    | Boolean x -> if x then "true" else "false"
    | Integer x -> x.ToString()
    | String x -> "\"" + x + "\""

  let renderIni (x : FeatureUnitValue) =
    match x with
    | Boolean x -> INIString <| if x then "true" else "false"
    | Integer x -> INIString <| x.ToString()
    | String x -> INIString <| Escape.escapeWithDoubleQuotes x

  let fromToml (toml : Nett.TomlObject) : Result<FeatureUnitValue, TomlError> =
    match toml with
    | :? Nett.TomlBool as v -> FeatureUnitValue.Boolean v.Value |> Result.Ok
    | :? Nett.TomlInt as v -> FeatureUnitValue.Integer v.Value |> Result.Ok
    | :? Nett.TomlString as v -> FeatureUnitValue.String v.Value |> Result.Ok
    | _ -> TomlError.UnexpectedType toml.ReadableTypeName |> Result.Error

module FeatureValue =
  let show (x : FeatureValue) =
    match x with
    | Value x -> x |> FeatureUnitValue.show
    | Dictionary x ->
      "{ " + (x
        |> Map.toSeq
        |> Seq.map (fun (a, b) -> a + " = " + (FeatureUnitValue.show b))
        |> String.concat ", "
      ) + " }"
    | Array x -> "[ " + (x |> Seq.map FeatureUnitValue.show |> String.concat ", ") + " ]"

  let renderIni (x : FeatureValue) =
    match x with
    | Value x -> x |> FeatureUnitValue.renderIni
    | Dictionary x ->
      x
      |> Map.toSeq
      |> Seq.collect (fun (key, value) ->
        [
          INIString <| Escape.escapeWithDoubleQuotes key;
          FeatureUnitValue.renderIni <| value;
        ]
      )
      |> List.ofSeq
      |> INIList
    | Array x ->
      x
      |> Seq.map FeatureUnitValue.renderIni
      |> List.ofSeq
      |> INIList

  let fromToml (toml : Nett.TomlObject) : Result<FeatureValue, TomlError> =
    match FeatureUnitValue.fromToml toml with
    | Result.Ok r -> FeatureValue.Value r |> Result.Ok
    | Result.Error e ->
      match toml with
      | :? Nett.TomlTable as v ->
        v.Keys
        |> Seq.map (fun key ->
          FeatureUnitValue.fromToml (v.Item key) |> Result.map(fun value -> (key, value))
        )
        |> all
        |> Result.map (List.rev >> Map.ofList >> FeatureValue.Dictionary)
      | :? Nett.TomlArray as v ->
        v.Items
        |> Seq.map FeatureUnitValue.fromToml
        |> all
        |> Result.map (List.rev >> FeatureValue.Array)
      | _ -> e |> Result.Error

module FeatureValueParse =
  open FParsec
  open System.Text.RegularExpressions

  let private ws = CharParsers.spaces
  let private str = CharParsers.skipString

  let trueParser : Parser<FeatureUnitValue, unit> =
    str "true" >>. (true |> FeatureUnitValue.Boolean |> Primitives.preturn)

  let falseParser =
    str "false" >>. (false |> FeatureUnitValue.Boolean |> Primitives.preturn)

  let booleanParser =
    trueParser <|> falseParser

  let integerParser = CharParsers.pint64 |>> FeatureUnitValue.Integer

  let private escapedParser =
    CharParsers.regex @"(?:[^'\\]|\\.)*"
  let private singleQuoteParser = str "'"

  let private removeEscapeRegex = Regex @"\\(.)"
  let private removeEscapeRegexReplacement = @"$1"

  let stringParser =
    singleQuoteParser >>. escapedParser .>> singleQuoteParser
    |>> fun x ->
      removeEscapeRegex.Replace(x, removeEscapeRegexReplacement)
      |> FeatureUnitValue.String

  let featureUnitValueParser =
    booleanParser
    <|> integerParser
    <|> stringParser

  let featureValueFromUnitParser = parse {
    let! value = featureUnitValueParser

    return value |> FeatureValue.Value
  }

  let private dictionaryKeyParser =
    CharParsers.regex @"\w[\w\d]*"

  let private dictionaryItemParser =
    Primitives.tuple2
      (dictionaryKeyParser .>> ws .>> (str "=") .>> ws)
      featureUnitValueParser

  let dictionaryParser =
    str "{"
    >>. ws
    >>. Primitives.sepEndBy1
      dictionaryItemParser
      (ws >>. (str ",") .>> ws)
    .>> ws
    .>> str "}"
    |>> (Map.ofList >> FeatureValue.Dictionary)

  let private arrayItemParser = parse {
    let! value = featureUnitValueParser

    return value
  }

  let arrayParser =
    str "["
    >>. ws
    >>. Primitives.sepEndBy1
      arrayItemParser
      (ws >>. (str ",") .>> ws)
    .>> ws
    .>> str "]"
    |>> FeatureValue.Array

  let featureValueParser =
    featureValueFromUnitParser
    <|> dictionaryParser
    <|> arrayParser
