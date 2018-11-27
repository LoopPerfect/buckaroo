namespace Buckaroo

module Solver = 

  open FSharp.Control

  type LocatedAtom = Atom * PackageLocation

  type Constraints = Set<Dependency>

  type Hints = AsyncSeq<LocatedAtom>

  type Depth = int

  type Visited = Set<LocatedAtom>

  type SolverState = (Solution * Constraints * Depth * Visited * Hints)

  type SearchStrategy = ISourceExplorer -> Hints -> Solution -> Constraints -> AsyncSeq<LocatedAtom>

  let private withTimeout timeout action =
    async {
      let! child = Async.StartChild (action, timeout)
      return! child
    }

  let versionSearchCost (v : Version) : int = 
    match v with 
    | Version.Revision _ -> 1
    | Version.Latest -> 2
    | Version.Tag _ -> 3
    | Version.SemVerVersion _ -> 4
    | Version.Branch _ -> 5

  let rec constraintSearchCost (c : Constraint) : int = 
    match c with 
    | Constraint.Exactly v -> versionSearchCost v
    | Constraint.Any xs -> 
      xs |> Seq.map constraintSearchCost |> Seq.append [ 9 ] |> Seq.min
    | Constraint.All xs -> 
      xs |> Seq.map constraintSearchCost |> Seq.append [ 0 ] |> Seq.max
    | Constraint.Complement _ -> 6
  
  let findConflicts (solution : Solution) (dependencies : Constraints) = seq {
    for dependency in dependencies do
      match solution |> Map.tryFind dependency.Package with 
      | Some resolvedVersion -> 
        if resolvedVersion.Version |> Constraint.agreesWith dependency.Constraint |> not
        then
          yield (dependency, resolvedVersion.Version)
      | None -> ()
  }

  let findUnsatisfied (solution : Solution) (dependencies : Constraints) = seq {
    for dependency in dependencies do
      match solution |> Map.tryFind dependency.Package with 
      | Some _ -> ()
      | None -> yield dependency
  }

  let agreesWith (dependencies : Constraints) (atom : Atom) = 
    dependencies 
    |> Seq.exists (fun dependency -> 
      dependency.Package = atom.Package && 
      atom.Version |> Constraint.agreesWith dependency.Constraint |> not)
    |> not

  let lockToHints (lock : Lock) : Hints = 
    lock.Packages 
    |> Map.toSeq
    |> Seq.map (fun (p, (v, location)) -> ({ Package = p; Version = v }, location))
    |> AsyncSeq.ofSeq

  let quickSearchStrategy (sourceExplorer : ISourceExplorer) (hints : Hints) solution dependencies = asyncSeq {
    let unsatisfied = 
      findUnsatisfied solution dependencies
      |> Seq.toList
      |> List.sortWith (fun x y -> Constraint.compare x.Constraint y.Constraint)

    for (atom, location) in hints do
      if unsatisfied |> Seq.exists (fun x -> Dependency.satisfies x atom)
      then
        yield (atom, location)

    for dependency in unsatisfied do
      let package = dependency.Package
      for version in sourceExplorer.FetchVersions package do
        if version |> Constraint.satisfies dependency.Constraint then
          let atom = { Package = package; Version = version }
          for location in sourceExplorer.FetchLocations package version do
            yield (atom, location)
  }

  let upgradeSearchStrategy (sourceExplorer : ISourceExplorer) (_ : Hints) solution dependencies = asyncSeq {
    let unsatisfied = 
      findUnsatisfied solution dependencies
      |> Seq.toList
      |> List.sortWith (fun x y -> Constraint.compare x.Constraint y.Constraint)

    for dependency in unsatisfied do
      let package = dependency.Package
      for version in sourceExplorer.FetchVersions package do
        if version |> Constraint.satisfies dependency.Constraint then
          let atom = { Package = package; Version = version }
          for location in sourceExplorer.FetchLocations package version do
            yield (atom, location)
  }

  let rec step (sourceExplorer : ISourceExplorer) (strategy : SearchStrategy) (state : SolverState) : AsyncSeq<Resolution> = asyncSeq {
    let (solution, dependencies, depth, visited, hints) = state

    let log (x : string) = 
      "[" + (string depth) + "] " + x
      |> System.Console.WriteLine

    let conflicts = 
      findConflicts solution dependencies
      |> Seq.toList

    if conflicts |> Seq.isEmpty
    then
      if findUnsatisfied solution dependencies |> Seq.isEmpty
      then
        yield Resolution.Ok solution
      else
        let atomsToExplore = 
          strategy sourceExplorer hints solution dependencies
          |> AsyncSeq.distinctUntilChanged
          |> AsyncSeq.filter (fun (atom, location) -> Set.contains (atom, location) visited |> not)
        
        for (atom, location) in atomsToExplore do
          try
            log("Exploring " + (Atom.show atom) + "@" + (PackageLocation.show location) + "...")
            
            // We pre-emptively grab the lock
            let! lockTask = 
              sourceExplorer.FetchLock location
              |> Async.StartChild

            let! manifest = sourceExplorer.FetchManifest location

            let resolvedVersion = {
              Version = atom.Version;
              Location = location;
              Manifest = manifest; 
            }
           
            let freshHints = 
              asyncSeq {
                try
                  let! lock = lockTask
                  yield! 
                    lock.Packages 
                    |> Map.toSeq
                    |> Seq.map (fun (p, (v, location)) -> ({ Package = p; Version = v }, location))
                    |> Seq.filter (fun (atom, location) -> 
                      Set.contains (atom, location) visited |> not && 
                      solution |> Map.containsKey atom.Package |> not && 
                      atom |> agreesWith dependencies
                    )
                    |> AsyncSeq.ofSeq
                with error ->
                  // log("Could not fetch buckaroo.lock.toml for " + (PackageLocation.show location))
                  // System.Console.WriteLine error
                  ()
              }

            let nextState = 
              (
                solution 
                |> Map.add atom.Package resolvedVersion, 
                dependencies
                |> Set.ofSeq
                |> Set.union manifest.Dependencies, 
                depth + 1, 
                visited |> Set.add (atom, location),
                hints |> AsyncSeq.append freshHints
              )
            yield! step sourceExplorer strategy nextState
          with error -> 
            log("Error exploring " + (Atom.show atom) + "@" + (PackageLocation.show location) + "...")
            System.Console.WriteLine(error)
            yield Resolution.Error error

        // We've run out of versions to try
        yield Resolution.Error (new System.Exception("No more versions to try! "))
    else
      yield
        conflicts
        |> Set.ofSeq
        |> Resolution.Conflict
  }

  let solutionCollector resolutions = 
    resolutions
    |> AsyncSeq.take 2048
    |> AsyncSeq.filter (fun x -> 
      match x with
      | Ok _ -> true
      | _ -> false
    )
    |> AsyncSeq.take 1
    |> AsyncSeq.toListAsync
    |> Async.RunSynchronously
    |> List.tryHead

  let solve (sourceExplorer : ISourceExplorer) (manifest : Manifest) (style : ResolutionStyle) (lock : Lock option) = async {
    let hints = 
      lock
      |> Option.map lockToHints
      |> Option.defaultValue AsyncSeq.empty
    let strategy = 
      match style with 
      | Quick -> quickSearchStrategy
      | Upgrading -> upgradeSearchStrategy
    let state = (Map.empty, manifest.Dependencies |> Set.ofSeq, 0, Set.empty, hints)
    let resolutions = 
      step sourceExplorer strategy state
    return
      resolutions
      |> solutionCollector
      |> Option.defaultValue (Set.empty |> Resolution.Conflict)
  }
