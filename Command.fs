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

  let readFile (path : string) = async {
    use sr = new IO.StreamReader(path)
    return! sr.ReadToEndAsync() |> Async.AwaitTask
  }

  let writeFile (path : string) (content : string) = async {
    use sw = new IO.StreamWriter(path)
    return! sw.WriteAsync(content) |> Async.AwaitTask
  }

  let readManifest = async {
    try 
      let! content = readFile ManifestFileName
      return 
        match Manifest.parse content with 
        | Result.Ok manifest -> manifest
        | Result.Error error -> new Exception("Error parsing manifest:\n" + error) |> raise
    with error -> return new Exception("Could not read project manifest. Are you sure you are in the right directory? ", error) |> raise
  }

  let writeManifest (manifest : Manifest) = async {
    // let content = Manifest.show manifest
    let content = Manifest.toToml manifest
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
      let sourceManager = new DefaultSourceManager()
      let! resolution = Solver.solve sourceManager newManifest
      do! writeManifest newManifest
      // TODO: Write lock file! 
      return ()
  }

  let resolve = async {
    let sourceManager = new DefaultSourceManager()
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
    let sourceManager = new DefaultSourceManager() :> ISourceManager
    let! versions = sourceManager.FetchVersions package
    for v in versions do
      Version.show v |> Console.WriteLine
    return ()
  }

  let buckarooDeps (sourceManager : ISourceManager) (lock : Lock) = async {
    let cellName package = 
      let (owner, project) = 
        match package with 
        | PackageIdentifier.GitHub x -> (x.Owner, x.Project)
        | PackageIdentifier.Adhoc x -> (x.Owner, x.Project)
      "buckaroo." + owner + "." + project
    let requiredPackages = 
      lock.Dependencies 
      |> Seq.map (fun d -> (cellName d.Package) + (Manifest.normalizeTarget d.Target))
      |> Seq.sort
      |> Seq.distinct
      |> Seq.toList
    return 
      "BUCKAROO_DEPS = [\n" + 
      (requiredPackages |> Seq.map (fun x -> "  \"" + x + "\"") |> String.concat ", \n") + 
      "\n]\n"
  }

  let installLockedPackage (lockedPackage : (PackageIdentifier * PackageLocation)) = async {
    let ( package, location ) = lockedPackage
    let ( owner, project ) = 
      match package with 
      | PackageIdentifier.GitHub x -> (x.Owner, x.Project)
      | PackageIdentifier.Adhoc x -> (x.Owner, x.Project)
    match location with 
    | GitHub gitHub -> 
      let gitUrl = PackageLocation.gitHubUrl gitHub.Package
      let! gitPath = Git.ensureClone gitUrl
      use repo = new Repository(gitPath)
      Commands.Checkout(repo, gitHub.Revision) |> ignore
      let installPath = Path.Combine(".", "buckaroo", owner, project)
      let! deletedExistingInstall = Files.deleteDirectoryIfExists installPath
      if deletedExistingInstall
      then "Deleted the existing folder at " + installPath |> Console.WriteLine
      let destination = installPath
      return! Files.copyDirectory gitPath destination
    | _ -> new Exception("Unsupported location type") |> raise
  }

  let install = async {
    let sourceManager = new DefaultSourceManager()
    let! lock = readLock
    for kv in lock.Packages do
      let project = kv.Key
      let exactLocation = kv.Value
      "Installing " + (PackageIdentifier.show project) + "... " |> Console.WriteLine
      do! installLockedPackage (project, exactLocation)
    let! buckarooDepsContent = buckarooDeps sourceManager lock 
    do! writeFile BuckarooDepsFileName buckarooDepsContent
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
