module Buckaroo.InstallCommand

open System
open System.IO
open Buckaroo.BuckConfig

let private fetchManifestFromLock (lock : Lock) (sourceExplorer : ISourceExplorer) (package : PackageIdentifier) = async {
  let (version, location) =  
    match lock.Packages |> Map.tryFind package with 
    | Some location -> location
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

let private buckarooDeps (dependencies : seq<TargetIdentifier>) = 
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

let private fetchBuckarooDepsContent (lock : Lock) (sourceExplorer : ISourceExplorer) (manifest : Manifest) = async {
  let! targets = fetchDependencyTargets lock sourceExplorer manifest
  return targets |> buckarooDeps
}

let private computeCellIdentifier (x : PackageIdentifier) = 
  (
    match x with 
    | PackageIdentifier.GitHub gitHub -> 
      [ "buckaroo"; "github"; gitHub.Owner; gitHub.Project ] 
    | PackageIdentifier.Adhoc adhoc -> 
      [ "buckaroo"; "adhoc"; adhoc.Owner; adhoc.Project ]
  )
  |> String.concat "."

let private installLockedPackage (lock : Lock) (gitManager : Git.GitManager) (sourceExplorer : ISourceExplorer) (lockedPackage : (PackageIdentifier * PackageLocation)) = async {
  let ( package, location ) = lockedPackage
  let installPath = packageInstallPath package
  match location with 
  | GitHub gitHub -> 
    let gitUrl = PackageLocation.gitHubUrl gitHub.Package
    let! gitPath = gitManager.Clone gitUrl
    do! gitManager.Fetch gitUrl gitHub.Revision
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
    let! targets = 
      fetchDependencyTargets lock sourceExplorer manifest
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
    let buckarooDepsPath = Path.Combine(installPath, Constants.BuckarooDepsFileName)
    let! buckarooDepsContent = fetchBuckarooDepsContent lock sourceExplorer manifest
    do! Files.writeFile buckarooDepsPath buckarooDepsContent
    // Write Buckaroo macros
    let buckarooMacrosPath = Path.Combine(installPath, Constants.BuckarooMacrosFileName)
    do! Files.writeFile buckarooMacrosPath buckarooMacros
  | _ -> 
    new Exception("Unsupported location type") |> raise
}

let task (context : Tasks.TaskContext) = async {
  let (gitManager, sourceExplorer) = context
  let! lock = Tasks.readLock
  do! 
    lock.Packages
    |> Seq.map (fun kvp -> async {
      let project = kvp.Key
      let (_, exactLocation) = kvp.Value
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
  let buckarooDepsPath = Path.Combine(Constants.BuckarooDepsFileName)
  let buckarooDepsContent = buckarooDeps lock.Dependencies
  do! Files.writeFile buckarooDepsPath buckarooDepsContent
  // Write Buckaroo macros
  let buckarooMacrosPath = Constants.BuckarooMacrosFileName
  do! Files.writeFile buckarooMacrosPath buckarooMacros
  return ()
}
