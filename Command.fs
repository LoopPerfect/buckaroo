module Command

open System
open System.IO
open FParsec

open Constants
open Solver
open LibGit2Sharp

type Dependency = Dependency.Dependency
type Manifest = Manifest.Manifest
type Project = Project.Project
type Lock = Lock.Lock
type ExactLocation = Lock.ExactLocation

type Command = 
| ListDependencies
| Resolve
| Install
| AddDependencies of List<Dependency>
| ShowVersions of Project

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

let installParser = parse {
  do! CharParsers.spaces
  do! CharParsers.skipString "install"
  do! CharParsers.spaces
  return Install
}

let addDependenciesParser = parse {
  do! CharParsers.spaces
  do! CharParsers.skipString "add"
  do! CharParsers.spaces1
  let! deps = Primitives.sepEndBy1 Dependency.parser CharParsers.spaces1
  return AddDependencies deps
}

let showVersionsParser = parse {
  do! CharParsers.spaces
  do! CharParsers.skipString "show-versions"
  do! CharParsers.spaces1
  let! project = Project.parser
  return ShowVersions project
}

let parser = 
  listDependenciesParser 
  <|> resolveParser
  <|> addDependenciesParser
  <|> installParser
  <|> showVersionsParser

let parse (x : string) : Result<Command, string> = 
  match run (parser .>> CharParsers.eof) x with
  | Success(result, _, _) -> Result.Ok result
  | Failure(error, _, _) -> Result.Error error

let readFile (path : string) = async {
  use sr = new IO.StreamReader(path)
  return! sr.ReadToEndAsync() |> Async.AwaitTask
}

let writeFile (path : string) (content : string) = async {
  use sw = new IO.StreamWriter(path)
  return! sw.WriteAsync(content) |> Async.AwaitTask
}

let readManifest = async {
  let! content = readFile ManifestFileName
  return 
    match Manifest.parse content with 
    | Result.Ok manifest -> manifest
    | Result.Error error -> new Exception("Error parsing manifest:\n" + error) |> raise
}

let writeManifest (manifest : Manifest) = async {
  let content = Manifest.show manifest
  return! writeFile ManifestFileName content
}

let readLock = async {
  let! content = readFile LockFileName
  match Lock.parse content with
  | Result.Ok lock -> return lock
  | Result.Error error -> 
    return new Exception("Error reading lock file. " + error) |> raise
}

let writeLock (lock : Lock) = async {
  let content = Lock.toToml lock
  return! writeFile LockFileName content
}

let listDependencies = async {
  let! manifest = readManifest
  manifest.Dependencies
  |> Seq.distinct
  |> Seq.map Dependency.show
  |> String.concat "\n"
  |> Console.WriteLine
  return ()
}

let add dependencies = async {
  let! manifest = readManifest
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
    let sourceManager = SourceManager.create () |> SourceManager.cached
    let! resolution = Solver.solve sourceManager newManifest
    do! writeManifest newManifest
    // TODO: Write lock file! 
    return ()
}

let resolve = async {
  let sourceManager = SourceManager.create () |> SourceManager.cached
  let! manifest = readManifest
  "Resolving dependencies... " |> Console.WriteLine
  let! resolution = Solver.solve sourceManager manifest
  match resolution with
  | Resolution.Conflict x -> 
    "Conflict! " |> Console.WriteLine
    x |> Console.WriteLine
    return ()
  | Resolution.Error e -> 
    "Error! " |> Console.WriteLine
    e |> Console.WriteLine
    return ()
  | Resolution.Ok solution -> 
    "Success! " |> Console.WriteLine
    let lock = Lock.fromManifestAndSolution manifest solution
    return! writeLock lock
}

let showVersions (project : Project) = async {
  let sourceManager = SourceManager.create () |> SourceManager.cached
  let! versions = sourceManager.FetchVersions project
  for v in versions do
    Version.show v |> Console.WriteLine
  return ()
}

let buckarooDeps (sourceManager : SourceManager) (lock : Lock) = async {
  let requiredPackages = 
    lock.Dependencies 
    |> Seq.toList
  let! buckDependencies = 
    requiredPackages 
    |> Seq.map (fun project -> async {
      match lock.Packages |> Map.tryFind project with
      | Some exactLocation -> 
        let! manifest = sourceManager.FetchManifest project exactLocation.Revision
        return 
          manifest.Target 
          |> Option.defaultValue (Project.defaultTarget project)
          |> Manifest.normalizeTarget
          |> (fun target -> (Project.cellName project) + target)
      | None -> 
        return raise <| new Exception((Project.show project) + " was not present in the lock")
    })
    |> Async.Parallel
  return 
    "BUCKAROO_DEPS = [\n" + 
    (buckDependencies |> Seq.map (fun x -> "  \"" + x + "\"") |> String.concat ", \n") + 
    "\n]\n"
}

let installLockedPackage (lockedPackage : (Project * ExactLocation)) = async {
  let ( project, exactLocation ) = lockedPackage
  let! gitPath = SourceManager.ensureClone exactLocation.Location
  use repo = new Repository(gitPath)
  Commands.Checkout(repo, exactLocation.Revision) |> ignore
  let installPath = Path.Combine(".", "buckaroo", (Project.installSubPath project))
  let! deletedExistingInstall = Files.deleteDirectoryIfExists installPath
  if deletedExistingInstall
  then "Deleted the existing folder at " + installPath |> Console.WriteLine
  let destination = installPath
  return! Files.copyDirectory gitPath destination
}

let install = async {
  let sourceManager = SourceManager.create () |> SourceManager.cached
  let! lock = readLock
  for kv in lock.Packages do
    let project = kv.Key
    let exactLocation = kv.Value
    "Installing " + (Project.show project) + "... " |> Console.WriteLine
    do! installLockedPackage (project, exactLocation)
  let! buckarooDepsContent = buckarooDeps sourceManager lock 
  do! writeFile BuckarooDepsFileName buckarooDepsContent
  return ()
}

let runCommand command = 
  match command with
  | ListDependencies -> listDependencies
  | Resolve -> resolve
  | Install -> install
  | AddDependencies dependencies -> add dependencies
  | ShowVersions project -> showVersions project
