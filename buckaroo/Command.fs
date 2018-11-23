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
  open FSharp.Control
  open Buckaroo.Constants
  open Buckaroo.Git
  open Buckaroo.BuckConfig

  let getCachePath () = 
    match System.Environment.GetEnvironmentVariable("BUCKAROO_CACHE_PATH") with 
    | null -> 
      let personalDirectory = 
        System.Environment.GetFolderPath Environment.SpecialFolder.Personal
      Path.Combine(personalDirectory, ".buckaroo", "cache")
    | path -> path
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
      let git = new GitCli()
      let cachePath = getCachePath ()
      let gitManager = new GitManager(git, cachePath)
      let sourceExplorer = new DefaultSourceExplorer(gitManager)
      let! resolution = Solver.solve sourceExplorer newManifest
      do! writeManifest newManifest
      // TODO: Write lock file! 
      return ()
  }

  let resolve = async {
    let git = new GitCli()
    let cachePath = getCachePath ()
    let gitManager = new GitManager(git, cachePath)
    let sourceExplorer = new LoggingSourceExplorer(new CachedSourceExplorer(new DefaultSourceExplorer(gitManager)))
    // let sourceExplorer = new CachedSourceExplorer(new DefaultSourceExplorer(gitManager))
    let! manifest = readManifest
    "Resolving dependencies... " |> Console.WriteLine
    let! resolution = Solver.solve sourceExplorer manifest
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
      do! async {
        try
          let! previousLock = readLock
          let diff = Lock.showDiff previousLock lock 
          System.Console.WriteLine(diff)
        with _ -> 
          ()
      }
      do! writeLock lock
      return ()
  }

  let showVersions (package : PackageIdentifier) = async {
    let git = new GitCli()
    let cachePath = getCachePath ()
    let gitManager = new GitManager(git, cachePath)
    let sourceExplorer = new DefaultSourceExplorer(gitManager) :> ISourceExplorer
    let! versions = 
      sourceExplorer.FetchVersions package
      |> AsyncSeq.toListAsync
    for v in versions do
      Version.show v |> Console.WriteLine
    return ()
  }

  let buckarooMacros = 
    [
      "def buckaroo_cell(package): "; 
      "  cell = native.read_config('buckaroo', package, '').strip()";
      "  if cell == '': "; 
      "    fail('Buckaroo does not have \"' + package + '\" installed. ')"; 
      "  return cell"; 
      ""; 
      "def buckaroo_deps(): ";
      "  raw = native.read_config('buckaroo', 'dependencies', '').split(' ')";
      "  return [ x.strip() for x in raw if len(x.strip()) > 0 ]"; 
      ""; 
      "def buckaroo_deps_from_package(package): "; 
      "  cell = buckaroo_cell(package)"; 
      "  all_deps = buckaroo_deps()"; 
      "  return [ x for x in all_deps if x.startswith(cell) ]"; 
      ""; 
    ]
    |> String.concat "\n"

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
    "print 'BUCKAROO_DEPS is deprecated; please use buckaroo_macros.bzl' \n\n" + 
    "BUCKAROO_DEPS = [\n" + 
    (requiredPackages |> Seq.map (fun x -> "  '" + x + "'") |> String.concat ", \n") + 
    "\n]\n"

  let packageInstallPath (package : PackageIdentifier) = 
    let (prefix, owner, project) = 
      match package with 
        | PackageIdentifier.GitHub x -> ("github", x.Owner, x.Project)
        | PackageIdentifier.Adhoc x -> ("adhoc", x.Owner, x.Project)
    Path.Combine(".", Constants.PackagesDirectory, prefix, owner, project)

  let fetchManifestFromLock (lock : Lock) (sourceExplorer : ISourceExplorer) (package : PackageIdentifier) = async {
    let location =  
      match lock.Packages |> Map.tryFind package with 
      | Some location -> location
      | None -> 
        new Exception("Lock file does not contain " + (PackageIdentifier.show package))
        |> raise
    return! sourceExplorer.FetchManifest location
  }

  let fetchDependencyTargets (lock : Lock) (sourceExplorer : ISourceExplorer) (manifest : Manifest) = async {
    let! targets =  
      manifest.Dependencies
      |> Seq.map (fun d -> async {
        match d.Target with 
        | Some target -> 
          return [ { Package = d.Package; Target = target } ] |> List.toSeq
        | None -> 
          let! manifest = fetchManifestFromLock lock sourceExplorer d.Package
          return 
            manifest.Targets 
            |> Seq.map (fun target -> { Package = d.Package; Target = target })
      })
      |> Async.Parallel
    return 
      targets 
      |> Seq.collect id
      |> List.ofSeq
  }

  let fetchBuckarooDepsContent (lock : Lock) (sourceExplorer : ISourceExplorer) (manifest : Manifest) = async {
    let! targets =  fetchDependencyTargets lock sourceExplorer manifest
    return targets
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

  let installLockedPackage (lock : Lock) (gitManager : GitManager) (sourceExplorer : ISourceExplorer) (lockedPackage : (PackageIdentifier * PackageLocation)) = async {
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
      let! manifest = fetchManifestFromLock lock sourceExplorer package
      // Touch .buckconfig
      let buckConfigPath = Path.Combine(installPath, ".buckconfig")
      if File.Exists buckConfigPath |> not 
      then 
        do! Files.writeFile buckConfigPath ""
      let! targets = fetchDependencyTargets lock sourceExplorer manifest
      // Write .buckconfig.d/.buckconfig.buckaroo
      let buckarooBuckConfigPath = 
        Path.Combine(installPath, ".buckconfig.d", ".buckconfig.buckaroo")
      let buckarooCells = 
        manifest.Dependencies
        |> Seq.map (fun d -> 
          let cell = computeCellIdentifier d.Package
          // TODO: Make this more robust using relative path computation 
          let path = Path.Combine("..", "..", "..", "..", (packageInstallPath d.Package))
          (cell, INIString path)
        )
        |> Seq.toList
      let buckarooSectionEntries = 
        manifest.Dependencies
        |> Seq.map (fun d -> 
          let cell = computeCellIdentifier d.Package
          (PackageIdentifier.show d.Package, INIString cell)
        )
        |> Seq.distinct
        |> Seq.toList
        |> List.append 
          [ ("dependencies", targets |> Seq.map (fun x -> ( computeCellIdentifier x.Package ) + (Target.show x.Target) ) |> String.concat " " |> INIString) ]
      let buckarooConfig : INIData = 
        Map.empty
        |> Map.add "repositories" (buckarooCells |> Map.ofSeq)
        |> Map.add "buckaroo" (buckarooSectionEntries |> Map.ofSeq)
      do! Files.mkdirp (Path.Combine(installPath, ".buckconfig.d"))
      do! Files.writeFile buckarooBuckConfigPath (buckarooConfig |> BuckConfig.render)
      // Write BUCKAROO_DEPS
      let buckarooDepsPath = Path.Combine(installPath, BuckarooDepsFileName)
      let! buckarooDepsContent = fetchBuckarooDepsContent lock sourceExplorer manifest
      do! Files.writeFile buckarooDepsPath buckarooDepsContent
      // Write Buckaroo macros
      let buckarooMacrosPath = Path.Combine(installPath, BuckarooMacrosFileName)
      do! Files.writeFile buckarooMacrosPath buckarooMacros
    | _ -> 
      new Exception("Unsupported location type") |> raise
  }

  let install = async {
    let git = new GitCli()
    let cachePath = getCachePath ()
    let gitManager = new GitManager(git, cachePath)
    let sourceExplorer = new DefaultSourceExplorer(gitManager)
    let! lock = readLock
    do! 
      lock.Packages
      |> Seq.map (fun kvp -> async {
        let project = kvp.Key
        let exactLocation = kvp.Value
        "Installing " + (PackageIdentifier.show project) + "... " |> Console.WriteLine
        do! installLockedPackage lock gitManager sourceExplorer (project, exactLocation)
      })
      |> Async.Parallel
      |> Async.Ignore
    // Touch .buckconfig
    let buckConfigPath = ".buckconfig"
    if File.Exists buckConfigPath |> not 
    then 
      do! Files.writeFile buckConfigPath ""
    // Write .buckconfig.d/.buckconfig.buckaroo
    let buckarooBuckConfigPath = 
      Path.Combine(".buckconfig.d", ".buckconfig.buckaroo")
    let buckarooRepositoriesCells = 
      lock.Packages
      |> Seq.map (fun kvp -> kvp.Key)
      |> Seq.map (fun t -> (computeCellIdentifier t, packageInstallPath t |> INIString))
    let buckarooSectionEntries = 
      lock.Dependencies
      |> Seq.map (fun d -> (PackageIdentifier.show d.Package, computeCellIdentifier d.Package |> INIString))
      |> Seq.distinct
      |> Seq.toList
      |> List.append 
        [ ("dependencies", lock.Dependencies |> Seq.map (fun x -> ( computeCellIdentifier x.Package ) + (Target.show x.Target) ) |> String.concat " " |> INIString) ]
    let buckarooConfig : INIData = 
      Map.empty
      |> Map.add "repositories" (buckarooRepositoriesCells |> Map.ofSeq)
      |> Map.add "buckaroo" (buckarooSectionEntries |> Map.ofSeq)
    do! Files.mkdirp ".buckconfig.d"
    do! Files.writeFile buckarooBuckConfigPath (buckarooConfig |> BuckConfig.render)
    // Write BUCKAROO_DEPS
    let buckarooDepsPath = Path.Combine(BuckarooDepsFileName)
    let buckarooDepsContent = buckarooDeps lock.Dependencies
    do! Files.writeFile buckarooDepsPath buckarooDepsContent
    // Write Buckaroo macros
    let buckarooMacrosPath = BuckarooMacrosFileName
    do! Files.writeFile buckarooMacrosPath buckarooMacros
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
