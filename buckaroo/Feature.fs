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
