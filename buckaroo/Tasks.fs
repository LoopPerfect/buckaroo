module Buckaroo.Tasks

open System
open System.IO
open System.Runtime
open FSharpx
open Buckaroo
open Buckaroo.Console
open Buckaroo.Logger

type TaskContext = {
  Console : ConsoleManager
  DownloadManager : DownloadManager
  GitManager : GitManager
  SourceExplorer : ISourceExplorer
  BuildSystem : BuildSystem
}

let private isWindows () =
  let description =
    InteropServices.RuntimeInformation.OSDescription
    |> String.toLower
  (String.contains "win" description) && not (String.contains "darwin" description)

let private determineBuildSystem (logger : Logger) = async {
  let useBazel =
    Environment.GetEnvironmentVariable "BUCKAROO_USE_BAZEL"
    |> isNull
    |> not

  if useBazel
  then
    return Bazel
  else
    let! hasBuckConfig = Files.exists ".buckconfig"

    if hasBuckConfig
    then
      logger.Warning "Using the Buck build-system since a .buckconfig file was found. Set BUCKAROO_USE_BAZEL to override this. "
      return Buck
    else
      return Bazel
}


let private getCachePath = async {
  return
    match System.Environment.GetEnvironmentVariable("BUCKAROO_CACHE_PATH") with
    | null ->
      let personalDirectory =
        System.Environment.GetFolderPath Environment.SpecialFolder.Personal
      Path.Combine(personalDirectory, ".buckaroo", "cache")
    | path -> path
}

let getContext loggingLevel fetchStyle = async {
  let consoleManager = ConsoleManager loggingLevel

  let! cachePath = getCachePath
  let downloadManager = DownloadManager (consoleManager, cachePath)

  let! hasGit =
    Bash.runBashSync "git" "version" ignore ignore
    |> Async.Catch
    |> Async.map
      (Choice.toOption
        >> Option.map (fun _ -> true)
        >> Option.defaultValue false)

  let useLibGit2 =
    not (isNull (Environment.GetEnvironmentVariable "BUCKAROO_USE_LIBGIT2"))
    || not hasGit

  let git =
    if useLibGit2
      then GitLib(consoleManager) :> IGit
      else GitCli(consoleManager) :> IGit

  let gitManager = GitManager(fetchStyle, consoleManager, git, cachePath)

  let! buildSystem = determineBuildSystem (createLogger consoleManager None)

  let sourceExplorer = DefaultSourceExplorer(consoleManager, downloadManager, gitManager, buildSystem)

  return {
    Console = consoleManager
    DownloadManager = downloadManager
    GitManager = gitManager
    SourceExplorer = sourceExplorer
    BuildSystem = buildSystem
  }
}

let readManifest (root : string) = async {
  try
    let! content = Files.readFile (Path.Combine(root, Constants.ManifestFileName))
    return
      match Manifest.parse content with
      | Result.Ok manifest -> manifest
      | Result.Error error ->
        Exception ("Error parsing manifest:\n" + (Manifest.ManifestParseError.show error))
        |> raise
  with error ->
    return
      Exception ("Could not read project manifest. Are you sure you are in the right directory? ", error)
      |> raise
}

let writeManifest (manifest : Manifest) = async {
  let content = Manifest.toToml manifest
  return! Files.writeFile Constants.ManifestFileName content
}

let readLock = async {
  if File.Exists Constants.LockFileName
  then
    let! content = Files.readFile Constants.LockFileName
    return
      match Lock.parse content with
      | Result.Ok lock -> lock
      | Result.Error error ->
        Exception("Error reading lock file. " + error) |> raise
  else
    return Exception("No lock file was found. Perhaps you need to run 'buckaroo resolve'?") |> raise
}

let readLockIfPresent = async {
  if File.Exists(Constants.LockFileName)
  then
    let! lock = readLock
    return Some lock
  else
    return None
}

let writeLock (lock : Lock) = async {
  let content = Lock.toToml lock
  return! Files.writeFile Constants.LockFileName content
}
