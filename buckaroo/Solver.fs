namespace Buckaroo

module Solver =

  open FSharp.Control
  open Buckaroo.Result

  type LocatedAtom = Atom * PackageLocation

  type Constraints = Set<Dependency>

  type Hints = AsyncSeq<LocatedAtom>

  type SolverState = {
    Solution : Solution;
    Constraints : Set<Dependency>;
    Depth : int;
    Visited : Set<LocatedAtom>;
    Locations : Map<AdhocPackageIdentifier, PackageSource>;
    Hints : AsyncSeq<LocatedAtom>;
  }

  type SearchStrategy = ISourceExplorer -> SolverState -> AsyncSeq<LocatedAtom>

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
      match solution.Resolutions |> Map.tryFind dependency.Package with
      | Some (resolvedVersion, _) ->
        if resolvedVersion.Version |> Constraint.agreesWith dependency.Constraint |> not
        then
          yield (dependency, resolvedVersion.Version)
      | None -> ()
  }

  let findUnsatisfied (solution : Solution) (dependencies : Constraints) = seq {
    for dependency in dependencies do
      match solution.Resolutions |> Map.tryFind dependency.Package with
      | Some _ -> ()
      | None -> yield dependency
  }

  let agreesWith (dependencies : Constraints) (atom : Atom) =
    dependencies
    |> Seq.exists (fun dependency ->
      dependency.Package = atom.Package &&
      atom.Version |> Constraint.agreesWith dependency.Constraint |> not)
    |> not

  let private lockToHints (lock : Lock) =
    lock.Packages
    |> Map.toSeq
    |> Seq.map (fun (k, v) -> ({ Package = k; Version = v.Version }, v.Location))

  let private mergeLocations (a : Map<AdhocPackageIdentifier, PackageSource>) (b : Map<AdhocPackageIdentifier, PackageSource>) =
    let folder state next = result {
      let (key : AdhocPackageIdentifier, source) = next
      let! s = state
      match (s |> Map.tryFind key, source) with
      | Some (PackageSource.Http l), PackageSource.Http r ->
        let conflicts =
          l
          |> Map.toSeq
          |> Seq.map (fun (v, s) -> (v, s, r.[v]))
          |> Seq.filter(fun (_, sl, sr) -> sl <> sr)
          |> Seq.toList

        match (conflicts |> List.length > 0) with
        | false ->
          return!
            Result.Error
              (ConflictingLocations (key, PackageSource.Http l, PackageSource.Http r))
        | true ->
          return s
            |> Map.add
              key
              (PackageSource.Http (Map(Seq.concat [ (Map.toSeq l) ; (Map.toSeq r) ])))

      | Some (PackageSource.Git _), PackageSource.Git _ ->
        return
          s
          |> Map.add key source
      | Some a, b ->
        return! Result.Error
          (ConflictingLocations (key, a, b))
      | None, _->
        return
          s
          |> Map.add key source
    }

    a
      |> Map.toSeq
      |> Seq.fold folder (Result.Ok b)

  let quickSearchStrategy (sourceExplorer : ISourceExplorer) (state : SolverState) = asyncSeq {
    let unsatisfied =
      findUnsatisfied state.Solution state.Constraints
      |> Seq.toList
      |> List.sortWith (fun x y -> Constraint.compare x.Constraint y.Constraint)

    for (atom, location) in state.Hints do
      if unsatisfied |> Seq.exists (fun x -> Dependency.satisfies x atom)
      then
        yield (atom, location)

    for dependency in unsatisfied do
      let package = dependency.Package
      for version in sourceExplorer.FetchVersions state.Locations package do
        if version |> Constraint.satisfies dependency.Constraint then
          let atom = { Package = package; Version = version }
          for location in sourceExplorer.FetchLocations state.Locations package version do
            yield (atom, location)
  }

  let upgradeSearchStrategy (sourceExplorer : ISourceExplorer) (state : SolverState) = asyncSeq {
    let unsatisfied =
      findUnsatisfied state.Solution state.Constraints
      |> Seq.toList
      |> List.sortWith (fun x y -> Constraint.compare x.Constraint y.Constraint)

    for dependency in unsatisfied do
      let package = dependency.Package
      for version in sourceExplorer.FetchVersions state.Locations package do
        if version |> Constraint.satisfies dependency.Constraint then
          let atom = { Package = package; Version = version }
          for location in sourceExplorer.FetchLocations state.Locations package version do
            yield (atom, location)
  }

  let rec private step (sourceExplorer : ISourceExplorer) (strategy : SearchStrategy) (state : SolverState) : AsyncSeq<Resolution> = asyncSeq {

    let log (x : string) =
      "[" + (string state.Depth) + "] " + x
      |> System.Console.WriteLine

    let conflicts =
      findConflicts state.Solution state.Constraints
      |> Seq.toList

    if conflicts |> Seq.isEmpty
    then
      if findUnsatisfied state.Solution state.Constraints |> Seq.isEmpty
      then
        yield Resolution.Ok state.Solution
      else
        let atomsToExplore =
          strategy sourceExplorer state
          |> AsyncSeq.distinctUntilChanged
          |> AsyncSeq.filter (fun (atom, location) -> Set.contains (atom, location) state.Visited |> not)

        for (atom, location) in atomsToExplore do
          try
            log("Exploring " + (Atom.show atom) + " -> " + (PackageLocation.show location) + "...")

            // We pre-emptively grab the lock
            let! lockTask =
              sourceExplorer.FetchLock location
              |> Async.StartChild

            log("Fetching manifest... ")

            let! manifest = sourceExplorer.FetchManifest location

            log("Got manifest " + (string manifest))

            let! mergedLocations = async {
              return
                match mergeLocations state.Locations manifest.Locations with
                | Result.Ok xs -> xs
                | Result.Error e -> raise (new System.Exception(e.ToString()))
            }

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
                    lock
                    |> lockToHints
                    |> Seq.filter (fun (atom, location) ->
                      Set.contains (atom, location) state.Visited |> not &&
                      state.Solution.Resolutions |> Map.containsKey atom.Package |> not &&
                      atom |> agreesWith state.Constraints
                    )
                    |> AsyncSeq.ofSeq
                with error ->
                  log("Could not fetch buckaroo.lock.toml for " + (PackageLocation.show location))
                  System.Console.WriteLine error
                  ()
              }

            let privatePackagesSolverState =
              {
                Solution = Solution.empty;
                Locations = Map.empty;
                Visited = Set.empty;
                Hints = state.Hints;
                Depth = state.Depth + 1;
                Constraints = manifest.PrivateDependencies;
              }

            let privatePackagesSolutions =
              step sourceExplorer strategy privatePackagesSolverState
              |> AsyncSeq.choose (fun resolution ->
                match resolution with
                | Resolution.Ok solution -> Some solution
                | _ -> None
              )

            yield!
              privatePackagesSolutions
              |> AsyncSeq.collect (fun privatePackagesSolution -> asyncSeq {
                let nextState = {
                  state with
                    Solution =
                      {
                        state.Solution with
                          Resolutions =
                            state.Solution.Resolutions
                            |> Map.add atom.Package (resolvedVersion, privatePackagesSolution)
                      };
                    Constraints =
                      state.Constraints
                      |> Set.ofSeq
                      |> Set.union manifest.Dependencies;
                    Depth = state.Depth + 1;
                    Visited =
                      state.Visited
                      |> Set.add (atom, location);
                    Locations = mergedLocations;
                    Hints =
                      state.Hints
                      |> AsyncSeq.append freshHints;
                }

                yield! step sourceExplorer strategy nextState
              })
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
      |> Option.map (lockToHints >> AsyncSeq.ofSeq)
      |> Option.defaultValue AsyncSeq.empty

    let strategy =
      match style with
      | Quick -> quickSearchStrategy
      | Upgrading -> upgradeSearchStrategy

    let state = {
      Solution = Solution.empty;
      Constraints =
        manifest.Dependencies
        |> Seq.append manifest.PrivateDependencies
        |> Set.ofSeq;
      Depth = 0;
      Visited = Set.empty;
      Locations = manifest.Locations;
      Hints = hints;
    }

    let resolutions =
      step sourceExplorer strategy state

    return
      resolutions
      |> solutionCollector
      |> Option.defaultValue (Set.empty |> Resolution.Conflict)
  }
