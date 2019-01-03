namespace Buckaroo

open FSharpx.Control.Async
open FSharpx.Collections
open Buckaroo.Tasks
open Buckaroo.RichOutput
open Buckaroo.Console

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
    let mutable isEmpty = true

    try
      for candidate in candidatesToExplore do
        isEmpty <- false
        yield Result.Ok (package, candidate)
      if isEmpty then
        match package with
        | PackageIdentifier.Adhoc _ -> ()
        | _ ->
          yield Result.Error (
            NotSatisfiable {
              Package = package;
              Constraint = constraints;
              Msg = "no versions found"
            }
          )
    with e ->
      yield Result.Error (
        NotSatisfiable {
          Package = package;
          Constraint = constraints;
          Msg = e.Message
        }
      )
  }

  let constraintsOf (ds: Set<Dependency>) =
    ds
    |> Seq.map (fun x -> (x.Package, x.Constraint))
    |> Seq.groupBy fst
    |> Seq.map (fun (k, xs) -> (k, xs |> Seq.map snd |> Set.ofSeq))
    |> Map.ofSeq

  let mergeConstraints (a: Constraints) (b: Constraints) =
    Seq.append
      (a |> Map.keys)
      (b |> Map.keys)
    |> Seq.map (fun k -> (k, ()))
    |> Map.ofSeq
    |> Map.map (fun k _ ->
      Set.union
        (a.TryFind(k) |> Option.defaultValue(Set[]))
        (b.TryFind(k) |> Option.defaultValue(Set[]))
    )

  let rec versionSearchCost (v : Version) : int =
    match v with
    | Version.SemVer _-> 0
    | Version.Git(GitVersion.Tag _) -> 1
    | Version.Git(GitVersion.Branch _)  -> 2
    | Version.Git(GitVersion.Revision _)  -> 3

  let rec constraintSearchCost (c : Constraint) : int =
    match c with
    | Constraint.Exactly v -> versionSearchCost v
    | Constraint.Any xs ->
      xs |> Seq.map constraintSearchCost |> Seq.append [ 9 ] |> Seq.min
    | Constraint.All xs ->
      xs |> Seq.map constraintSearchCost |> Seq.append [ 0 ] |> Seq.max
    | Constraint.Complement _ -> 6

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



  let rec private step (context : TaskContext) (strategy : SearchStrategy) (state : SolverState) : AsyncSeq<Resolution> = asyncSeq {

    let sourceExplorer = context.SourceExplorer

    let log (x : string) =
      (
        "[" + (string state.Depth) + "] "
        |> RichOutput.text
        |> RichOutput.foreground System.ConsoleColor.DarkGray
      ) +
      (x |> RichOutput.text)
      |> context.Console.Write

    let unsatisfied =
      findUnsatisfied state.Solution state.Constraints

    if Seq.isEmpty unsatisfied
      then
        yield Resolution.Ok state.Solution
      else
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
              yield Resolution.Error (new System.Exception ("no location found for: " + p.ToString() + "that could satisfy: " + c.ToString()))
          | Result.Ok (package, (packageLock, versions)) ->
            try
              log("Exploring (" + state.Depth.ToString() + ") " + (PackageIdentifier.show package) + " -> " + (string packageLock) + "...")

              // We pre-emptively grab the lock
              let! lockTask =
                sourceExplorer.FetchLock packageLock
                |> Async.StartChild

              log("Fetching manifest... ")

              let! manifest = sourceExplorer.FetchManifest packageLock

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

              let freshHints =
                asyncSeq {
                  try
                    let! lock = lockTask
                    yield!
                      lock
                      |> lockToHints
                      |> Seq.filter (fun (atom, packageLock) ->
                        Set.contains (atom.Package, packageLock) state.Visited |> not &&
                        state.Solution.Resolutions |> Map.containsKey atom.Package |> not
                      )
                      |> AsyncSeq.ofSeq
                  with error ->
                    log("Could not fetch buckaroo.lock.toml for " + (string packageLock))
                    log(string error)
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
                      // Note: union will favour kvp from the second map
                      // however, this is a private resoluton and we can resolve independently
                      // TODO: introduce strict / hybrid mode where we can tweak this behaviour
                      Constraints =
                        Map.union
                          state.Constraints
                          (constraintsOf manifest.Dependencies)

                      Depth = state.Depth + 1;
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
                |> AsyncSeq.map(fun resolution ->
                  match resolution with
                  | Resolution.Failure f ->
                    if state.Constraints.ContainsKey f.Package &&
                      match f.Constraint with
                      | All xs -> xs |> List.forall state.Constraints.[f.Package].Contains
                      | x -> state.Constraints.[f.Package].Contains x
                    then
                      resolution
                    else
                      log("trying different resolution to workaround: " + f.ToString())
                      // we backtracked far enough, we can proceed normally...
                      // TODO: remember f.Package + f.Constraint always fails
                      Resolution.Error (new System.Exception (f.ToString()))
                  | x -> x
                )
                |> AsyncSeq.takeWhileInclusive(fun resolution ->
                  match resolution with
                  | Resolution.Failure f ->
                    log("backtracking due to failure " + f.ToString())
                    false
                  | _ -> true
                )

            with error ->
              log("Error exploring " + (string packageLock) + "...")
              log(string error)
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
      | _ -> false
    )
    |> AsyncSeq.take 1
    |> AsyncSeq.toListAsync
    |> Async.RunSynchronously
    |> List.tryHead

  let solve (context : TaskContext) (manifest : Manifest) (style : ResolutionStyle) (lock : Lock option) = async {
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
