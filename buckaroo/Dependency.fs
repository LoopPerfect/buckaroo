namespace Buckaroo

type Dependency =
  {
    Package : PackageIdentifier
    Constraint : Constraint
    Targets : Target list option
  }

module Dependency =

  open FParsec
  open Buckaroo.RichOutput

  let satisfies (dependency : Dependency) (atom : Atom) =
    atom.Package = dependency.Package && atom.Versions |> Constraint.satisfies dependency.Constraint

  let show (x : Dependency) =
    (PackageIdentifier.show x.Package) + "@" + Constraint.show x.Constraint +
      (
        x.Targets
        |> Option.map (fun xs -> "[ " + (xs |> Seq.map Target.show |> String.concat " ") + " ]")
        |> Option.defaultValue ""
      )

  let showRich (x : Dependency) =
    (
      (
        PackageIdentifier.show x.Package
        |> text
        |> foreground System.ConsoleColor.Magenta
      ) +
      " at " +
      (
        Constraint.show x.Constraint
        |> text
        |> foreground System.ConsoleColor.Magenta
      )
    ) +
    (
      x.Targets
      |> Option.map (fun xs ->
        (RichOutput.text "[ ") +
        (xs
          |> Seq.map Target.show
          |> String.concat " "
          |> RichOutput.text
          |> RichOutput.foreground System.ConsoleColor.Green) +
        " ]"
      )
      |> Option.defaultValue (RichOutput.text "")
    )

  let parser = parse {
    let! p = PackageIdentifier.parser
    do! CharParsers.skipString "@"
    let! c = Constraint.parser
    return
      { Package = p; Constraint = c; Targets = None }
  }

  let parse (x : string) : Result<Dependency, string> =
    match run (parser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(errorMsg, _, _) -> Result.Error errorMsg
