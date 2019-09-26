namespace Buckaroo

type Override =
  {
    Package : PackageIdentifier
    Substitution : PackageIdentifier
  }

module Override =

  open FSharpx.Result
  open Buckaroo.Toml

  let fromToml (toml : Nett.TomlObject) = result {
    let! table =
      toml
      |> Toml.asTable

    let! package =
      table
      |> Toml.get "package"
      |> Result.bind Toml.asString
      |> Result.bind (PackageIdentifier.parse >> Result.mapError TomlError.UnexpectedType)

    let! substitution =
      table
      |> Toml.get "substitution"
      |> Result.bind Toml.asString
      |> Result.bind (PackageIdentifier.parse >> Result.mapError TomlError.UnexpectedType)

    return
      {
        Package = package
        Substitution = substitution
      }
  }
