namespace Buckaroo

type Command = 
| Init
| ListDependencies
| Resolve
| Install
| AddDependencies of List<Dependency>
| ShowVersions of PackageIdentifier

module Command = 

  open System
  open System.IO
  open FParsec
  open LibGit2Sharp
  open Buckaroo.Constants
  open Buckaroo.Git

  let initParser : Parser<Command, Unit> = parse {
    do! CharParsers.spaces
    do! CharParsers.skipString "init"
    do! CharParsers.spaces
    return Init
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
    let! project = PackageIdentifier.parser
    return ShowVersions project
  }

  let parser = 
    listDependenciesParser 
    <|> resolveParser
    <|> addDependenciesParser
    <|> installParser
    <|> showVersionsParser
    <|> initParser

  let parse (x : string) : Result<Command, string> = 
    match run (parser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(error, _, _) -> Result.Error error

  let readManifest = async {
    try 
      let! content = Files.readFile ManifestFileName
      return 
        match Manifest.parse content with 
        | Result.Ok manifest -> manifest
        | Result.Error error -> new Exception("Error parsing manifest:\n" + error) |> raise
    with error -> return new Exception("Could not read project manifest. Are you sure you are in the right directory? ", error) |> raise
  }

  let writeManifest (manifest : Manifest) = async {
    // let content = Manifest.show manifest
    let content = Manifest.toToml manifest
    return! Files.writeFile ManifestFileName content
  }

  let readLock = async {
    if File.Exists LockFileName
    then 
      let! content = Files.readFile LockFileName
      return
        match Lock.parse content with
        | Result.Ok lock -> lock
        | Result.Error error -> 
          new Exception("Error reading lock file. " + error) |> raise
    else 
      return new Exception("No lock file was found. Perhaps you need to run 'buckaroo resolve'?") |> raise
  }

  let writeLock (lock : Lock) = async {
    let content = Lock.toToml lock
    return! Files.writeFile LockFileName content
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
      let gitManager = new GitManager("cache")
      let sourceManager = new DefaultSourceManager(gitManager)
      let! resolution = Solver.solve sourceManager newManifest
      do! writeManifest newManifest
      // TODO: Write lock file! 
      return ()
  }

  let resolve = async {
    let gitManager = new GitManager("cache")
    let sourceManager = new DefaultSourceManager(gitManager)
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

  let showVersions (package : PackageIdentifier) = async {
    let gitManager = new GitManager("cache")
    let sourceManager = new DefaultSourceManager(gitManager) :> ISourceManager
    let! versions = sourceManager.FetchVersions package
    for v in versions do
      Version.show v |> Console.WriteLine
    return ()
  }

  let buckarooDeps (dependencies : seq<TargetIdentifier>) = 
    let cellName package = 
      let (prefix, owner, project) = 
        match package with 
        | PackageIdentifier.GitHub x -> ("github", x.Owner, x.Project)
        | PackageIdentifier.Adhoc x -> ("adhoc", x.Owner, x.Project)
      "buckaroo." + prefix + "." + owner + "." + project
    let requiredPackages = 
      dependencies 
      |> Seq.map (fun d -> (cellName d.Package) + (Target.show d.Target))
      |> Seq.sort
      |> Seq.distinct
      |> Seq.toList
    "BUCKAROO_DEPS = [\n" + 
    (requiredPackages |> Seq.map (fun x -> "  '" + x + "'") |> String.concat ", \n") + 
    "\n]\n"

  let packageInstallPath (package : PackageIdentifier) = 
    let (prefix, owner, project) = 
      match package with 
        | PackageIdentifier.GitHub x -> ("github", x.Owner, x.Project)
        | PackageIdentifier.Adhoc x -> ("adhoc", x.Owner, x.Project)
    Path.Combine(".", Constants.PackagesDirectory, prefix, owner, project)

  let fetchManifestFromLock (lock : Lock) (sourceManager : ISourceManager) (package : PackageIdentifier) = async {
    let location =  
      match lock.Packages |> Map.tryFind package with 
      | Some location -> location
      | None -> 
        new Exception("Lock file does not contain " + (PackageIdentifier.show package))
        |> raise
    return! sourceManager.FetchManifest location
  }

  let fetchBuckarooDepsContent (lock : Lock) (sourceManager : ISourceManager) (manifest : Manifest) = async {
    let! targets =  
      manifest.Dependencies
      |> Seq.map (fun d -> async {
        match d.Target with 
        | Some target -> 
          return [ { Package = d.Package; Target = target } ] |> List.toSeq
        | None -> 
          let! manifest = fetchManifestFromLock lock sourceManager d.Package
          return 
            manifest.Targets 
            |> Seq.map (fun target -> { Package = d.Package; Target = target })
      })
      |> Async.Parallel
    return targets
      |> Seq.collect id
      |> buckarooDeps
  }

  let computeCellIdentifier (x : PackageIdentifier) = 
    (
      match x with 
      | PackageIdentifier.GitHub gitHub -> 
        [ "buckaroo"; "github"; gitHub.Owner; gitHub.Project ] 
      | PackageIdentifier.Adhoc adhoc -> 
        [ "buckaroo"; "adhoc"; adhoc.Owner; adhoc.Project ]
    )
    |> String.concat "."

  let installLockedPackage (lock : Lock) (gitManager : GitManager) (sourceManager : ISourceManager) (lockedPackage : (PackageIdentifier * PackageLocation)) = async {
    let ( package, location ) = lockedPackage
    let installPath = packageInstallPath package
    match location with 
    | GitHub gitHub -> 
      let gitUrl = PackageLocation.gitHubUrl gitHub.Package
      let! gitPath = gitManager.Clone gitUrl
      use repo = new Repository(gitPath)
      Commands.Checkout(repo, gitHub.Revision) |> ignore
      let! deletedExistingInstall = Files.deleteDirectoryIfExists installPath
      if deletedExistingInstall
      then "Deleted the existing folder at " + installPath |> Console.WriteLine
      let destination = installPath
      do! Files.copyDirectory gitPath destination
      let! manifest = fetchManifestFromLock lock sourceManager package
      // Touch .buckconfig
      let buckConfigPath = Path.Combine(installPath, ".buckconfig")
      let! buckConfig = async {
        if File.Exists buckConfigPath
        then 
          let! content = Files.readFile buckConfigPath
          let parse = (content.Trim()) |> FS.INIReader.INIParser.read2res
          match parse with 
          | ParserResult.Success (config, _, _) -> 
            return config
          | ParserResult.Failure (error, _, _) -> 
            return!
              new Exception("Invalid .buckconfig for " + (PackageIdentifier.show package) + "\n" + error)
              |> raise
        else
          return Map.empty
      }
      let buckarooCells = 
        manifest.Dependencies
        |> Seq.map (fun d -> 
          let cell = computeCellIdentifier d.Package
          // TODO: Make this more robust using relative path computation 
          let path = Path.Combine("..", "..", "..", "..", (packageInstallPath d.Package))
          (cell, path)
        )
        |> Seq.toList
      let patchedBuckConfig = 
        buckConfig 
        |> BuckConfig.removeBuckarooEntries
        |> BuckConfig.addCells buckarooCells
      do! Files.writeFile buckConfigPath (patchedBuckConfig |> BuckConfig.render)
      // Write BUCKAROO_DEPS
      let buckarooDepsPath = Path.Combine(installPath, BuckarooDepsFileName)
      let! buckarooDepsContent = fetchBuckarooDepsContent lock sourceManager manifest
      do! Files.writeFile buckarooDepsPath buckarooDepsContent
    | _ -> 
      new Exception("Unsupported location type") |> raise
  }

  let install = async {
    let gitManager = new GitManager("cache")
    let sourceManager = new DefaultSourceManager(gitManager)
    let! lock = readLock
    for kv in lock.Packages do
      let project = kv.Key
      let exactLocation = kv.Value
      "Installing " + (PackageIdentifier.show project) + "... " |> Console.WriteLine
      do! installLockedPackage lock gitManager sourceManager (project, exactLocation)
    // Write .buckconfig
    let buckConfigPath = ".buckconfig"
    let! buckConfig = async {
      if File.Exists buckConfigPath 
      then 
        let! content = Files.readFile buckConfigPath
        return 
          match FS.INIReader.INIParser.read2res content with 
          | ParserResult.Success (config, _, _) -> config
          | ParserResult.Failure (error, _, _) -> new Exception(error) |> raise
      else 
        return Map.empty
    }
    let buckarooCells = 
      lock.Packages 
      |> Seq.map (fun kvp -> (computeCellIdentifier kvp.Key, packageInstallPath kvp.Key))
    let patchedBuckConfig = 
      buckConfig
      |> BuckConfig.removeBuckarooEntries
      |> BuckConfig.addCells buckarooCells
    do! Files.writeFile buckConfigPath (patchedBuckConfig |> BuckConfig.render)
    // Write BUCKAROO_DEPS
    let buckarooDepsContent = buckarooDeps (lock.Dependencies |> Set.toSeq) 
    do! Files.writeFile BuckarooDepsFileName buckarooDepsContent
    return ()
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

  let runCommand command = 
    match command with
    | Init -> init
    | ListDependencies -> listDependencies
    | Resolve -> resolve
    | Install -> install
    | AddDependencies dependencies -> add dependencies
    | ShowVersions project -> showVersions project
