module Buckaroo.Tasks

open System
open System.IO
open Buckaroo.Console

type TaskContext = {
  Console : ConsoleManager; 
  DownloadManager : DownloadManager; 
  GitManager : GitManager; 
  SourceExplorer : ISourceExplorer;
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

let getContext = async {
  let consoleManager = new ConsoleManager(LoggingLevel.Trace)

  let! cachePath = getCachePath
  let downloadManager = new DownloadManager(consoleManager, cachePath)

  let useLibGit2 = System.Environment.GetEnvironmentVariable("BUCKAROO_USE_LIBGIT2") <> null
  
  let git = 
    if useLibGit2  
      then new GitLib(consoleManager) :> IGit
      else new GitCli(consoleManager) :> IGit
  
  let gitManager = new GitManager(git, cachePath)
  let sourceExplorer = new DefaultSourceExplorer(downloadManager, gitManager)

  return {
    Console = consoleManager; 
    DownloadManager = downloadManager; 
    GitManager = gitManager; 
    SourceExplorer = sourceExplorer;
  }
}

let readManifest (root : string) = async {
  try 
    let! content = Files.readFile (Path.Combine(root, Constants.ManifestFileName))
    return 
      match Manifest.parse content with 
      | Result.Ok manifest -> manifest
      | Result.Error error -> 
        new Exception("Error parsing manifest:\n" + (Manifest.ManifestParseError.show error)) 
        |> raise
  with error -> 
    return 
      new Exception("Could not read project manifest. Are you sure you are in the right directory? ", error) 
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
        new Exception("Error reading lock file. " + error) |> raise
  else 
    return new Exception("No lock file was found. Perhaps you need to run 'buckaroo resolve'?") |> raise
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
