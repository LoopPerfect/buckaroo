namespace Buckaroo

open Tasks
open Console

type Command =
| Start
| Help
| Init
| Version
| Resolve
| Install
| Upgrade
| Quickstart
| AddDependencies of List<Dependency>
| RemoveDependencies of List<PackageIdentifier>
| ShowCompletions

module Command =

  open System
  open System.IO
  open FParsec

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
    return Resolve
  }

  let upgradeParser = parse {
    do! CharParsers.spaces
    do! CharParsers.skipString "upgrade"
    do! CharParsers.spaces
    return Upgrade
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

  let removeDependenciesParser = parse {
    do! CharParsers.spaces
    do! CharParsers.skipString "remove"
    do! CharParsers.spaces1
    let! deps = Primitives.sepEndBy1 PackageIdentifier.parser CharParsers.spaces1
    return RemoveDependencies deps
  }

  let showCompletionsParser : Parser<Command, Unit> = parse {
    do! CharParsers.spaces
    do! CharParsers.skipString "show-completions"
    do! CharParsers.spaces
    return ShowCompletions
  }

  let parser = parse {
    let! command =
      resolveParser
      <|> upgradeParser
      <|> addDependenciesParser
      <|> removeDependenciesParser
      <|> installParser
      <|> quickstartParser
      <|> initParser
      <|> versionParser
      <|> helpParser
      <|> showCompletionsParser
      <|> startParser

    let! isVerbose = verboseParser

    return (command, if isVerbose then LoggingLevel.Trace else LoggingLevel.Info)
  }

  let parse (x : string) =
    match run (parser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error

  let upgrade context = async {
    // TODO: Roll-back on failure!
    do! ResolveCommand.task context ResolutionStyle.Upgrading
    do! InstallCommand.task context
  }

  let init context = async {
    let path = Constants.ManifestFileName
    if File.Exists(path) |> not
    then
      use sw = File.CreateText(path)
      sw.Write(Manifest.zero |> Manifest.show)
      context.Console.Write("Wrote " + Constants.ManifestFileName)
    else
      new Exception("There is already a manifest in this directory") |> raise
  }

  let runCommand loggingLevel command = async {
    let! context = Tasks.getContext loggingLevel

    do!
      match command with
      | Start -> StartCommand.task context
      | Init -> init context
      | Help -> HelpCommand.task context
      | Version -> VersionCommand.task context
      | Resolve -> ResolveCommand.task context ResolutionStyle.Quick
      | Upgrade -> upgrade context
      | Install -> InstallCommand.task context
      | Quickstart -> QuickstartCommand.task context
      | AddDependencies dependencies -> AddCommand.task context dependencies
      | RemoveDependencies dependencies -> RemoveCommand.task context dependencies
      | ShowCompletions -> ShowCompletions.task context

    do! context.Console.Flush()
  }
