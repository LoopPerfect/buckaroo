namespace Buckaroo

open FSharpx.Collections
open Buckaroo.Tasks
open Buckaroo.Console
open RichOutput
open FSharp.Control

module Solver =

  open FSharp.Control
  open Buckaroo.Result

  type LocatedAtom = Atom * PackageLocation

  type Constraints = Map<PackageIdentifier, Set<Constraint>>

  type SolverState = {
    Solution : Solution;
    Constraints : Constraints;
    Depth : int;
    Visited : Set<PackageIdentifier * PackageLock>;
    Locations : Map<AdhocPackageIdentifier, PackageSource>;
    Hints : AsyncSeq<Atom * PackageLock>;
  }

  type SearchStrategyError =
  | NotSatisfiable of NotSatisfiable
  | NoLocationAvaiable of PackageIdentifier * Constraint

  type LocatedVersionSet = PackageLocation * Set<Version>

  type SearchStrategy = ISourceExplorer -> SolverState -> AsyncSeq<Result<PackageIdentifier * LocatedVersionSet, SearchStrategyError>>
  let private withTimeout timeout action =
    async {
      let! child = Async.StartChild (action, timeout)
      return! child
    }
  let fetchCandidatesForConstraint sourceExplorer locations package constraints = asyncSeq {
    let candidatesToExplore = SourceExplorer.fetchLocationsForConstraint sourceExplorer locations package constraints

    for x in candidatesToExplore do
      yield
        match x with
        | Candidate (a,b) -> Result.Ok (package, (a, b))
        | Unsatisfiable u -> Result.Error (SearchStrategyError.NotSatisfiable {
          Package = package;
          Constraint = u
          Msg = "Constraint not satisfiable"
        })
  }

  let constraintsOf (ds: Set<Dependency>) =
    ds
    |> Seq.map (fun x -> (x.Package, x.Constraint))
    |> Seq.groupBy fst
    |> Seq.map (fun (k, xs) -> (k, xs |> Seq.map snd |> Set.ofSeq))
    |> Map.ofSeq

  let findUnsatisfied (solution : Solution) (dependencies : Constraints) = seq {
    yield! Set.difference
      (dependencies |> Map.keys |> Set.ofSeq)
      (solution.Resolutions |> Map.keys  |> Set.ofSeq)

    let maybeSatisfied =
      Set.intersect
        (dependencies |> Map.keys |> Set.ofSeq)
        (solution.Resolutions |> Map.keys  |> Set.ofSeq)

    yield!
      maybeSatisfied
      |> Set.toSeq
      |> Seq.map (fun package ->
        (package,
         Constraint.satisfies
           (Constraint.All (dependencies.[package] |> Set.toList ))
           (fst solution.Resolutions.[package]).Versions ))
      |> Seq.filter(snd >> not)
      |> Seq.map fst
  }

  let private lockToHints (lock : Lock) =
    lock.Packages
    |> Map.toSeq
    |> Seq.map (fun (k, v) -> ({ Package = k; Versions = v.Versions }, v.Location))

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
      |> Set.ofSeq

    yield!
      state.Hints
      |> AsyncSeq.filter (fun (atom, _) -> unsatisfied |> Set.contains atom.Package)
      |> AsyncSeq.map (fun (atom, lock) ->
        Result.Ok (atom.Package, (PackageLock.toLocation lock, atom.Versions))
      )

    for package in unsatisfied do
      let constraints =
        state.Constraints
        |> Map.tryFind package
        |> Option.defaultValue Set.empty
        |> Seq.toList
        |> Constraint.All

      yield! fetchCandidatesForConstraint sourceExplorer state.Locations package constraints

  }

  let upgradeSearchStrategy (sourceExplorer : ISourceExplorer) (state : SolverState) = asyncSeq {
    let unsatisfied = findUnsatisfied state.Solution state.Constraints

    for package in unsatisfied do
      let constraints =
        state.Constraints
        |> Map.tryFind package
        |> Option.defaultValue Set.empty
        |> Seq.toList
        |> Constraint.All

      yield! fetchCandidatesForConstraint sourceExplorer state.Locations package constraints
  }

  let private printManifestInfo log (state: SolverState) (manifest:Manifest) =
    let newDepCount =
      manifest.Dependencies
        |> Seq.filter(fun (x : Dependency) -> state.Constraints.ContainsKey x.Package |> not)
        |> Seq.length

    if newDepCount > 0 then
      log(("Manifest introduces " |> text) +
        (manifest.Dependencies
          |> Seq.filter(fun (x : Dependency) -> state.Constraints.ContainsKey x.Package |> not)
          |> Seq.length
          |> string
          |> info) +
        ((" new dependenc" + if newDepCount > 1 then "ies" else "y") |> text), LoggingLevel.Info)

  let private candidateToAtom (sourceExplorer : ISourceExplorer) (state: SolverState) (package, (location, versions)) = async {
    let! packageLock = sourceExplorer.LockLocation location
    return (package, (packageLock, versions))
  }
  let private filterAtom state (package, (packageLock, _)) = (
    (Set.contains (package, packageLock) state.Visited |> not) &&
    (match state.Solution.Resolutions |> Map.tryFind package with
      | Some (rv, _) -> rv.Lock = packageLock
      | None -> true)
  )

  let private recoverOrFail state log resolutions =
    resolutions
    |> AsyncSeq.map (fun resolution ->
        match resolution with
        | Resolution.Failure f ->
          if state.Constraints.ContainsKey f.Package &&
            match f.Constraint with
            | All xs -> xs |> List.forall state.Constraints.[f.Package].Contains
            | x -> state.Constraints.[f.Package].Contains x
          then resolution
          else
            log("Trying different resolution to workaround: " + f.ToString()|>text, LoggingLevel.Info)
            // we backtracked far enough, we can proceed normally...
            // TODO: remember f.Package + f.Constraint always fails
            Resolution.Error (new System.Exception (f.ToString()))
        | x -> x
    )
    |> AsyncSeq.takeWhileInclusive (fun resolution ->
      match resolution with
      | Resolution.Failure f ->
        log("Backtracking due to failure " + f.ToString() |> text, LoggingLevel.Debug)
        false
      | _ -> true
    )

  let rec private step (context : TaskContext) (strategy : SearchStrategy) (state : SolverState) : AsyncSeq<Resolution> = asyncSeq {

    let sourceExplorer = context.SourceExplorer

    let log = namespacedLogger context.Console "solver"

    let unsatisfied =
      findUnsatisfied state.Solution state.Constraints
      |> Seq.toList

    if Seq.isEmpty unsatisfied
      then
        yield Resolution.Ok state.Solution
      else
        let totalDeps = state.Constraints |> Map.count
        let satisfiedDepsCount = totalDeps - (unsatisfied |> Seq.length)
        log( ("Resolved " |> text) +
          (satisfiedDepsCount.ToString() |> highlight) +
          (subtle "/") +
          (totalDeps.ToString() |> highlight),
          LoggingLevel.Info)

        let atomsToExplore =
          strategy sourceExplorer state
          |> AsyncSeq.mapAsync (fun x ->
            match x with
            | Result.Ok candidate ->
              candidate
              |> candidateToAtom sourceExplorer state
              |> fun x -> async {
                let! result = x;
                return Result.Ok result; }
            | Result.Error e -> async { return Result.Error e }
          )
          |> AsyncSeq.filter (fun x ->
            match x with
            | Result.Ok atom -> filterAtom state atom
            | _ -> true)


        for x in atomsToExplore do
          match x with
          | Result.Error e ->
            match e with
            | NotSatisfiable x ->
              // Package + Constraint is unresolvable
              // we need to skip every branch that requires Package + Constraint
              yield Resolution.Failure x
            | NoLocationAvaiable (p, c)  ->
              yield Resolution.Error (new System.Exception ("No location found for: " + p.ToString() + " that could satisfy: " + c.ToString()))
          | Result.Ok (package, (packageLock, versions)) ->
            try
              log(("Exploring " |> text) + (PackageIdentifier.showRich package) + "...", LoggingLevel.Info)

              // We pre-emptively grab the lock
              let! lockTask =
                sourceExplorer.FetchLock (packageLock, versions)
                |> Async.StartChild

              log("Fetching manifest..." |> text, LoggingLevel.Info)
              let manifestFetchStart = System.DateTime.Now
              let! manifest = sourceExplorer.FetchManifest (packageLock, versions)
              let manifestFetchEnd = System.DateTime.Now
              log((success "success ") +
                  ("Manifest fetched in " |> text) +
                  (info ((manifestFetchEnd - manifestFetchStart).TotalSeconds.ToString("N3") + "s")),
                  LoggingLevel.Info)
              printManifestInfo log state manifest

              let! mergedLocations = async {
                return
                  match mergeLocations state.Locations manifest.Locations with
                  | Result.Ok xs -> xs
                  | Result.Error e -> raise (new System.Exception(e.ToString()))
              }

              let resolvedVersion = {
                Versions = versions;
                Lock = packageLock;
                Manifest = manifest;
              }

              let versionSetStr =
                packageLock
                |> PackageLock.toLocation
                |> PackageLocation.versionSetFromLocation
                |> Set.union versions
                |> Version.showRichSet

              log ( (success "success ") + (text "Resolved ") + (PackageIdentifier.showRich package) + (subtle " -> ") + versionSetStr, LoggingLevel.Info)

              let freshHints =
                asyncSeq {
                  try
                    log( (text "Fetching lock-file for ") + (PackageIdentifier.showRich package) + "...", LoggingLevel.Debug)
                    let! lock = lockTask
                    log( (success "success ") + (text "Fetched the lock-file for ") + (PackageIdentifier.showRich package), LoggingLevel.Info)
                    yield!
                      lock
                      |> lockToHints
                      |> Seq.filter (fun (atom, packageLock) ->
                        Set.contains (atom.Package, packageLock) state.Visited |> not &&
                        state.Solution.Resolutions |> Map.containsKey atom.Package |> not
                      )
                      |> AsyncSeq.ofSeq
                  with error ->
                    log(string error|>text, LoggingLevel.Debug)
                    ()
                }

              let privatePackagesSolverState =
                {
                  Solution = Solution.empty;
                  Locations = Map.empty;
                  Visited = Set.empty;
                  Hints = state.Hints;
                  Depth = state.Depth + 1;
                  Constraints = constraintsOf manifest.PrivateDependencies
                }

              let privatePackagesSolutions =
                step context strategy privatePackagesSolverState
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
                              |> Map.add package (resolvedVersion, privatePackagesSolution)
                        };

                      Constraints =
                        manifest.Dependencies
                        |> Seq.fold
                          (fun m (dep : Dependency) ->
                            Map.insertWith
                              Set.union
                              dep.Package
                              (Set[dep.Constraint])
                              m)
                          state.Constraints


                      Depth = state.Depth;
                      Visited =
                        state.Visited
                        |> Set.add (package, packageLock);
                      Locations = mergedLocations;
                      Hints =
                        state.Hints
                        |> AsyncSeq.append freshHints;
                  }

                  yield! step context strategy nextState
                })
                |> recoverOrFail state log

            with error ->
              log("Error exploring " + (string packageLock) + "..." |> text, LoggingLevel.Debug)
              log(string error |> text, LoggingLevel.Debug)
              yield Resolution.Error error
  }

  let solutionCollector resolutions =
    resolutions
    |> AsyncSeq.take (1024)
    |> AsyncSeq.takeWhileInclusive (fun x ->
      match x with
      | Failure _ -> false
      | _ -> true)
    |> AsyncSeq.filter (fun x ->
      match x with
      | Ok _ -> true
      | Failure _ -> true
      | _ -> false)
    |> AsyncSeq.take 1
    |> AsyncSeq.toListAsync
    |> Async.RunSynchronously
    |> List.tryHead

  let solve (context : TaskContext) (partialSolution : Solution) (manifest : Manifest) (style : ResolutionStyle) (lock : Lock option) = async {
    let hints =
      lock
      |> Option.map (lockToHints >> AsyncSeq.ofSeq)
      |> Option.defaultValue AsyncSeq.empty

    let strategy =
      match style with
      | Quick -> quickSearchStrategy
      | Upgrading -> upgradeSearchStrategy

    let state = {
      Solution = partialSolution;
      Constraints =
        Set.unionMany [ manifest.Dependencies; manifest.PrivateDependencies ]
        |> constraintsOf
      Depth = 0;
      Visited = Set.empty;
      Locations = manifest.Locations;
      Hints = hints;
    }

    let resolutions =
      step context strategy state

    let result =
      resolutions
      |> solutionCollector
      |> Option.defaultValue (Set.empty |> Resolution.Conflict)

    context.Console.Write(string result, LoggingLevel.Trace)

    return result
  }


  let rec fromLock (sourceExplorer : ISourceExplorer) (lock : Lock) : Async<Solution> = async {
    let rec packageLockToSolution (locked : LockedPackage) : Async<ResolvedVersion * Solution> = async {
      let! manifest = sourceExplorer.FetchManifest (locked.Location, locked.Versions)
      let! resolutions =
        locked.PrivatePackages
          |> Map.toSeq
          |> AsyncSeq.ofSeq
          |> AsyncSeq.mapAsync (fun (k, lock) -> async {
            let! solution = packageLockToSolution lock
            return (k, solution)
          })
          |> AsyncSeq.toListAsync

      let resolvedVersion : ResolvedVersion = {
        Versions = locked.Versions;
        Lock = locked.Location;
        Manifest = manifest;
      }

      return (resolvedVersion, { Resolutions = resolutions |> Map.ofSeq })
    }

    let! resolutions =
      lock.Packages
      |> Map.toSeq
      |> AsyncSeq.ofSeq
      |> AsyncSeq.mapAsync(fun (package, lockedPakckage) -> async {
        let! solution = lockedPakckage |> packageLockToSolution
        return (package, solution)
      })
      |> AsyncSeq.toListAsync

    return {
      Resolutions = resolutions |> Map.ofSeq
    }
  }

  let unlock (solution : Solution) (packages : Set<PackageIdentifier>) : Solution = {
    Resolutions =
      solution.Resolutions
        |> Map.toSeq
        |> Seq.filter (fst >> packages.Contains >> not)
        |> Map.ofSeq
  }
