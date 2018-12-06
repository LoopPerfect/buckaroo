namespace Buckaroo

type Command = 
| Start
| Help
| Init
| ListDependencies
| Resolve
| Install
| Upgrade
| Quickstart
| AddDependencies of List<Dependency>
| RemoveDependencies of List<PackageIdentifier>
| ShowVersions of PackageIdentifier

module Command = 

  open System
  open System.IO
  open FSharp.Control
  open FParsec
  open Buckaroo.Constants

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

  let listDependenciesParser : Parser<Command, Unit> = parse {
    do! CharParsers.spaces
    do! CharParsers.skipString "list"
    do! CharParsers.spaces
    return ListDependencies
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

  let showVersionsParser = parse {
    do! CharParsers.spaces
    do! CharParsers.skipString "show-versions"
    do! CharParsers.spaces1
    let! project = PackageIdentifier.parser
    return ShowVersions project
  }

  let parser = 
    listDependenciesParser 
    <|> resolveParser
    <|> upgradeParser
    <|> addDependenciesParser
    <|> removeDependenciesParser
    <|> installParser
    <|> quickstartParser
    <|> showVersionsParser
    <|> initParser
    <|> helpParser
    <|> startParser

  let parse (x : string) : Result<Command, string> = 
    match run (parser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error

  let listDependencies = async {
    let! manifest = Tasks.readManifest "."
    manifest.Dependencies
    |> Seq.distinct
    |> Seq.map Dependency.show
    |> String.concat "\n"
    |> Console.WriteLine
    return ()
  }

  let showVersions (context : Tasks.TaskContext) (package : PackageIdentifier) = async {
    let sourceExplorer = context.SourceExplorer

    let! versions = 
      sourceExplorer.FetchVersions Map.empty package
      |> AsyncSeq.toListAsync
    for v in versions do
      Version.show v |> Console.WriteLine
    return ()
  }

  let add (context : Tasks.TaskContext) dependencies = async {
    let sourceExplorer = context.SourceExplorer

    let! manifest = Tasks.readManifest "."
    let newManifest = { 
      manifest with 
        Dependencies = 
          manifest.Dependencies 
          |> Seq.append dependencies 
          |> Set.ofSeq;
    }
    if manifest = newManifest 
    then return ()
    else 
      let! maybeLock = async {
        if File.Exists(Constants.LockFileName)
        then
          let! lock = Tasks.readLock
          return Some lock
        else
          return None
      }
      let! resolution = Solver.solve sourceExplorer newManifest ResolutionStyle.Quick maybeLock 
      match resolution with
      | Resolution.Ok solution -> 
        do! Tasks.writeManifest newManifest
        do! Tasks.writeLock (Lock.fromManifestAndSolution newManifest solution)
        do! InstallCommand.task context
      | _ -> ()
      System.Console.WriteLine ("Success. ")
      return ()
  }

  let upgrade context = async {
    // TODO: Roll-back on failure! 
    do! ResolveCommand.task context ResolutionStyle.Upgrading
    do! InstallCommand.task context
  }

  let init = async {
    let path = ManifestFileName
    if File.Exists(path) |> not
    then
      use sw = File.CreateText(path)
      sw.Write(Manifest.zero |> Manifest.show)
      System.Console.WriteLine("Wrote " + ManifestFileName)
    else 
      new Exception("There is already a manifest in this directory") |> raise
  }

  let runCommand command = async {
    let! context = Tasks.getContext
    do! 
      match command with
      | Start -> StartCommand.task
      | Init -> init
      | Help -> HelpCommand.task
      | ListDependencies -> listDependencies
      | Resolve -> ResolveCommand.task context ResolutionStyle.Quick
      | Upgrade -> upgrade context
      | Install -> InstallCommand.task context
      | Quickstart -> QuickstartCommand.task context
      | AddDependencies dependencies -> add context dependencies
      | RemoveDependencies dependencies -> RemoveCommand.task context dependencies
      | ShowVersions project -> showVersions context project
  }
