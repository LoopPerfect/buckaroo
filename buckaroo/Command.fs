namespace Buckaroo

open FSharp.Control
open Buckaroo.Console
open Buckaroo.Tasks

type Command =
| Start
| Help
| Init
| Version
| Resolve of ResolutionStyle
| Install
| Quickstart
| UpgradeDependencies of List<PackageIdentifier>
| AddDependencies of List<Dependency>
| RemoveDependencies of List<PackageIdentifier>
| ShowCompletions

module Command =

  open System
  open System.IO
  open FParsec
  open Buckaroo.RichOutput

  let verboseParser : Parser<bool, Unit> = parse {
    let! maybeSkip =
      CharParsers.skipString "--verbose"
      |> Primitives.opt
    return Option.isSome maybeSkip
  }

  let startParser : Parser<Command, Unit> = parse {
    do! CharParsers.spaces
    return Start
  }

  let initParser : Parser<Command, Unit> = parse {
    do! CharParsers.spaces
    do! CharParsers.skipString "init"
    do! CharParsers.spaces
    return Init
  }

  let helpParser : Parser<Command, Unit> = parse {
    do! CharParsers.spaces
    do! CharParsers.skipString "help"
    do! CharParsers.spaces
    return Help
  }

  let versionParser = parse {
    do! CharParsers.spaces
    do! CharParsers.skipString "version"
    do! CharParsers.spaces
    return Command.Version
  }

  let resolveParser = parse {
    do! CharParsers.spaces
    do! CharParsers.skipString "resolve"
    do! CharParsers.spaces

    let! strategy =
      parse {
        do! CharParsers.skipString "--upgrade"

        return ResolutionStyle.Upgrading
      }
      |> Primitives.opt

    return Resolve (strategy |> Option.defaultValue ResolutionStyle.Quick)
  }

  let installParser = parse {
    do! CharParsers.spaces
    do! CharParsers.skipString "install"
    do! CharParsers.spaces
    return Install
  }

  let quickstartParser = parse {
    do! CharParsers.spaces
    do! CharParsers.skipString "quickstart"
    do! CharParsers.spaces
    return Quickstart
  }

  let addDependenciesParser = parse {
    do! CharParsers.spaces
    do! CharParsers.skipString "add"
    do! CharParsers.spaces1

    let! deps = Primitives.sepEndBy1 Dependency.parser CharParsers.spaces1

    return AddDependencies deps
  }

  let upgradeDepenenciesParser = parse {
    do! CharParsers.skipString "upgrade"

    let! packages =
      (attempt >> Primitives.many)
        <| parse {
          do! CharParsers.spaces1
          return! PackageIdentifier.parser
        }

    return UpgradeDependencies packages
  }

  let removeDependenciesParser = parse {
    do! CharParsers.spaces
    do! CharParsers.skipString "remove"
    do! CharParsers.spaces1
    let! deps = Primitives.sepBy PackageIdentifier.parser CharParsers.spaces1
    do! CharParsers.spaces
    return RemoveDependencies deps
  }

  let showCompletionsParser : Parser<Command, Unit> = parse {
    do! CharParsers.spaces
    do! CharParsers.skipString "show-completions"
    do! CharParsers.spaces
    return ShowCompletions
  }

  let parser = parse {
    do! CharParsers.spaces

    let! command =
      resolveParser
      <|> upgradeDepenenciesParser
      <|> addDependenciesParser
      <|> removeDependenciesParser
      <|> installParser
      <|> quickstartParser
      <|> initParser
      <|> versionParser
      <|> helpParser
      <|> showCompletionsParser
      <|> startParser

    do! CharParsers.spaces

    let! isVerbose = verboseParser

    do! CharParsers.spaces

    let loggingLevel = if isVerbose then LoggingLevel.Trace else LoggingLevel.Info

    return (command, loggingLevel)
  }

  let parse (x : string) =
    match run (parser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error

  let add (context : Tasks.TaskContext) dependencies = async {
    let! manifest = Tasks.readManifest "."
    let newManifest = {
      manifest with
        Dependencies =
          manifest.Dependencies
          |> Seq.append dependencies
          |> Set.ofSeq;
    }

    if manifest = newManifest
    then
      return ()
    else
      let! maybeLock = async {
        if File.Exists (Constants.LockFileName)
        then
          let! lock = Tasks.readLock
          return Some lock
        else
          return None
      }

      let! resolution = Solver.solve context Solution.empty newManifest ResolutionStyle.Quick maybeLock

      match resolution with
      | Resolution.Ok solution ->
        do! Tasks.writeManifest newManifest
        do! Tasks.writeLock (Lock.fromManifestAndSolution newManifest solution)
        do! InstallCommand.task context
      | _ -> ()

      System.Console.WriteLine ("Success. ")

      return ()
  }

  let init context = async {
    let path = Constants.ManifestFileName
    if File.Exists(path) |> not
    then
      use sw = File.CreateText(path)
      sw.Write(Manifest.zero |> Manifest.show)
      context.Console.Write("Wrote " + Constants.ManifestFileName)
    else
      context.Console.Write( ("warning " |> warn) + ("There is already a buckaroo.toml file in this directory" |> text))
  }

  let runCommand loggingLevel command = async {
    let! context = Tasks.getContext loggingLevel

    do!
      match command with
      | Start -> StartCommand.task context
      | Init -> init context
      | Help -> HelpCommand.task context
      | Version -> VersionCommand.task context
      | Resolve style -> ResolveCommand.task context Solution.empty style
      | Install -> InstallCommand.task context
      | Quickstart -> QuickstartCommand.task context
      | UpgradeDependencies dependencies -> UpgradeCommand.task context dependencies
      | AddDependencies dependencies -> AddCommand.task context dependencies
      | RemoveDependencies dependencies -> RemoveCommand.task context dependencies
      | ShowCompletions -> ShowCompletions.task context

    do! context.Console.Flush()
  }
