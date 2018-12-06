module Buckaroo.InstallCommand

open System
open System.IO
open Buckaroo.PackageLocation
open Buckaroo.BuckConfig

let private fetchManifestFromLock (lock : Lock) (sourceExplorer : ISourceExplorer) (package : PackageIdentifier) = async {
  let location =  
    match lock.Packages |> Map.tryFind package with 
    | Some lockedPackage -> lockedPackage.Location
    | None -> 
      new Exception("Lock file does not contain " + (PackageIdentifier.show package))
      |> raise
  
  return! sourceExplorer.FetchManifest location
}

let packageInstallPath (package : PackageIdentifier) = 
  let (prefix, owner, project) = 
    match package with 
      | PackageIdentifier.GitHub x -> ("github", x.Owner, x.Project)
      | PackageIdentifier.Adhoc x -> ("adhoc", x.Owner, x.Project)
  Path.Combine(".", Constants.PackagesDirectory, prefix, owner, project)

let private fetchDependencyTargets (lock : Lock) (sourceExplorer : ISourceExplorer) (manifest : Manifest) = async {
  let! targetIdentifiers = 
    manifest.Dependencies
    |> Seq.map (fun d -> async {
      let! targets = 
        match d.Targets with 
        | Some targets -> async {
            return targets |> List.toSeq
          }
        | None -> async {
            let! manifest = fetchManifestFromLock lock sourceExplorer d.Package
            return manifest.Targets |> Set.toSeq
          }
      return
        targets
        |> Seq.map (fun target -> 
          { Package = d.Package; Target = target }
        )
    })
    |> Async.Parallel
  return 
    targetIdentifiers
    |> Seq.collect id 
    |> Seq.toList
}

let private buckarooMacros = 
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

let private computeCellIdentifier (x : PackageIdentifier) = 
  (
    match x with 
    | PackageIdentifier.GitHub gitHub -> 
      [ "buckaroo"; "github"; gitHub.Owner; gitHub.Project ] 
    | PackageIdentifier.Adhoc adhoc -> 
      [ "buckaroo"; "adhoc"; adhoc.Owner; adhoc.Project ]
  )
  |> String.concat "."

let installPackageSources (context : Tasks.TaskContext) (installPath : string) (location : PackageLocation) = async {
  let downloadManager = context.DownloadManager
  let gitManager = context.GitManager
  match location with 
  | GitHub gitHub -> 
    let gitUrl = PackageLocation.gitHubUrl gitHub.Package
    do! gitManager.FetchCommit gitUrl gitHub.Revision (hintToBranch gitHub.Hint)
    do! gitManager.CopyFromCache gitUrl gitHub.Revision installPath
  | Http http -> 
    let! pathToCache = downloadManager.DownloadToCache http.Url
    let! discoveredHash = Files.sha256 pathToCache
    if discoveredHash <> http.Sha256
    then
      return 
        new Exception("Hash mismatch for " + http.Url + "! Expected " + http.Sha256 + "but found " + discoveredHash)
        |> raise
    do! Files.mkdirp installPath
    do! Archive.extractTo pathToCache installPath http.StripPrefix
  | _ -> 
    new Exception("Unsupported location type") |> raise
}

// let rec private installPackage (context : Tasks.TaskContext) (root : string) (package : PackageIdentifier) (lockedPackage : LockedPackage) = async {
//   let installPath = Path.Combine(root, packageInstallPath package lockedPackage.Location)
//   do! installPackageSources context installPath lockedPackage.Location

//   for (k, v) in lockedPackage.PrivatePackages |> Map.toSeq do
//     do! installPackage context installPath k v
// }

let private generateBuckConfig (sourceExplorer : ISourceExplorer) (manifest : Manifest) packages (pathToCell : string) = async {
  let pathToPackage (package : PackageIdentifier) = 
    Path.Combine(
      (".." + (string Path.DirectorySeparatorChar)) |> String.replicate ((Paths.depth pathToCell) + 1), // + 1 for .buckconfig.d
      (packageInstallPath package))
    |> Paths.normalize

  let repositoriesCells = 
    packages
    |> Map.toSeq
    |> Seq.filter (fun (k, _) -> manifest.Dependencies |> Seq.exists (fun d -> d.Package = k))
    |> Seq.map (fun (k, _) -> (computeCellIdentifier k, pathToPackage k |> INIString))
    |> Map.ofSeq
  
  let! targets = 
    manifest.Dependencies
    |> Seq.map (fun dependency -> async {
      let lockedPackage = 
        packages
        |> Map.find dependency.Package

      let! packageManifest = sourceExplorer.FetchManifest lockedPackage.Location

      let targets = 
        dependency.Targets
        |> Option.defaultValue (List.ofSeq packageManifest.Targets)

      return (dependency.Package, targets)
    })
    |> Async.Parallel

  let dependencies = 
    targets
    |> Seq.collect (fun (p, ts) -> ts |> Seq.map (fun t -> (p, t)))
    |> Seq.distinct
    |> Seq.map (fun (package, target) -> (computeCellIdentifier package) + (Target.show target) ) 
    |> String.concat " " 
    |> INIString

  let buckarooSectionEntries = 
    manifest.Dependencies
    |> Seq.map (fun d -> (PackageIdentifier.show d.Package, computeCellIdentifier d.Package |> INIString))
    |> Seq.distinct
    |> Seq.toList
    |> List.append [ ("dependencies", dependencies) ]
    |> Map.ofSeq
  
  let buckarooConfig = 
    Map.empty
    |> Map.add "repositories" repositoriesCells
    |> Map.add "buckaroo" buckarooSectionEntries
  
  return buckarooConfig |> BuckConfig.render
}

let rec private installPackages (context : Tasks.TaskContext) (root : string) (packages : Map<PackageIdentifier, LockedPackage>) = async {
  // Prepare workspace
  do! Files.touch (Path.Combine(root, ".buckconfig"))
  do! Files.writeFile (Path.Combine(root, Constants.BuckarooMacrosFileName)) buckarooMacros

  let! manifest = Tasks.readManifest root

  // Install packages
  for (package, lockedPackage) in packages |> Map.toSeq do
    let installPath = Path.Combine(root, packageInstallPath package)

    do! installPackageSources context installPath lockedPackage.Location
    do! installPackages context installPath lockedPackage.PrivatePackages

    // Write .buckconfig.d for child package
    let! packageManifest = Tasks.readManifest installPath
    let! buckarooConfig = 
      generateBuckConfig context.SourceExplorer packageManifest (packages |> Map.remove package) installPath
    
    do! Files.mkdirp (Path.Combine(installPath, ".buckconfig.d"))
    do! Files.writeFile (Path.Combine(installPath, ".buckconfig.d", ".buckconfig.buckaroo")) buckarooConfig
}

// let private installPackage (context : Tasks.TaskContext) (lock : Lock) (lockedPackage : (PackageIdentifier * PackageLocation)) = async {
//   let gitManager = context.GitManager
//   let sourceExplorer = context.SourceExplorer

//   let ( package, location ) = lockedPackage
//   let installPath = packageInstallPath package location

//   do! Files.deleteDirectoryIfExists installPath |> Async.Ignore
//   do! installPackageSources context installPath location
    
//   let! manifest = fetchManifestFromLock lock sourceExplorer package
//   // Touch .buckconfig
//   let buckConfigPath = Path.Combine(installPath, ".buckconfig")
//   if File.Exists buckConfigPath |> not 
//   then 
//     do! Files.writeFile buckConfigPath ""
//   let! targets = 
//     fetchDependencyTargets lock sourceExplorer manifest
//   // Write .buckconfig.d/.buckconfig.buckaroo
//   let buckarooBuckConfigPath = 
//     Path.Combine(installPath, ".buckconfig.d", ".buckconfig.buckaroo")
//   let buckarooCells = 
//     manifest.Dependencies
//     |> Seq.map (fun d -> 
//       let cell = computeCellIdentifier d.Package
//       // TODO: Make this more robust using relative path computation 
//       let path = Path.Combine("..", "..", "..", "..", "..", (packageInstallPath d.Package d.))
//       (cell, INIString path)
//     )
//     |> Seq.toList
//   let buckarooSectionEntries = 
//     manifest.Dependencies
//     |> Seq.map (fun d -> 
//       let cell = computeCellIdentifier d.Package
//       (PackageIdentifier.show d.Package, INIString cell)
//     )
//     |> Seq.distinct
//     |> Seq.toList
//     |> List.append 
//       [ ("dependencies", targets |> Seq.map (fun x -> ( computeCellIdentifier x.Package ) + (Target.show x.Target) ) |> String.concat " " |> INIString) ]
//   let buckarooConfig : INIData = 
//     Map.empty
//     |> Map.add "repositories" (buckarooCells |> Map.ofSeq)
//     |> Map.add "buckaroo" (buckarooSectionEntries |> Map.ofSeq)
//   do! Files.mkdirp (Path.Combine(installPath, ".buckconfig.d"))
//   do! Files.writeFile buckarooBuckConfigPath (buckarooConfig |> BuckConfig.render)
//   // Write BUCKAROO_DEPS
//   let buckarooDepsPath = Path.Combine(installPath, Constants.BuckarooDepsFileName)
//   let! buckarooDepsContent = fetchBuckarooDepsContent lock sourceExplorer manifest
//   do! Files.writeFile buckarooDepsPath buckarooDepsContent
//   // Write Buckaroo macros
//   let buckarooMacrosPath = Path.Combine(installPath, Constants.BuckarooMacrosFileName)
//   do! Files.writeFile buckarooMacrosPath buckarooMacros
// }

let task (context : Tasks.TaskContext) = async {
  let! lock = Tasks.readLock

  do! installPackages context "." lock.Packages

  // do! 
  //   lock.Packages
  //   |> Map.toSeq
  //   |> Seq.map (fun (k, v) -> async {
  //     "Installing " + (PackageIdentifier.show k) + "... " |> Console.WriteLine
  //     do! installPackage context "." k v
  //   })
  //   |> Async.Parallel
  //   |> Async.Ignore
  
  // Touch .buckconfig
  // let buckConfigPath = ".buckconfig"
  // if File.Exists buckConfigPath |> not 
  // then 
  //   do! Files.writeFile buckConfigPath ""
  
  // Write .buckconfig.d/.buckconfig.buckaroo
  // let buckarooBuckConfigPath = 
  //   Path.Combine(".buckconfig.d", ".buckconfig.buckaroo")
  // let buckarooRepositoriesCells = 
  //   lock.Packages
  //   |> Seq.map (fun kvp -> kvp.Key)
  //   |> Seq.map (fun t -> (computeCellIdentifier t, packageInstallPath t |> INIString))
  // let buckarooSectionEntries = 
  //   lock.Dependencies
  //   |> Seq.map (fun d -> (PackageIdentifier.show d.Package, computeCellIdentifier d.Package |> INIString))
  //   |> Seq.distinct
  //   |> Seq.toList
  //   |> List.append 
  //     [ ("dependencies", lock.Dependencies |> Seq.map (fun x -> ( computeCellIdentifier x.Package ) + (Target.show x.Target) ) |> String.concat " " |> INIString) ]
  // let buckarooConfig : INIData = 
  //   Map.empty
  //   |> Map.add "repositories" (buckarooRepositoriesCells |> Map.ofSeq)
  //   |> Map.add "buckaroo" (buckarooSectionEntries |> Map.ofSeq)
  // do! Files.mkdirp ".buckconfig.d"
  // do! Files.writeFile buckarooBuckConfigPath (buckarooConfig |> BuckConfig.render)

  // Write BUCKAROO_DEPS
  // let buckarooDepsPath = Path.Combine(Constants.BuckarooDepsFileName)
  // let buckarooDepsContent = buckarooDeps lock.Dependencies
  // do! Files.writeFile buckarooDepsPath buckarooDepsContent

  // Write Buckaroo macros
  // let buckarooMacrosPath = Constants.BuckarooMacrosFileName
  // do! Files.writeFile buckarooMacrosPath buckarooMacros
  
  return ()
}
