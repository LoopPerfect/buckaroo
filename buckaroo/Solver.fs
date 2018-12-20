namespace Buckaroo
open FSharpx.Collections

open Buckaroo.Tasks
open Buckaroo.RichOutput
open Console

module Solver =

  open FSharp.Control
  open Buckaroo.Result

  type LocatedAtom = Atom * PackageLocation

  type Constraints = Map<PackageIdentifier, Set<Constraint>>

  type Hints = AsyncSeq<LocatedAtom>

  type SolverState = {
    Solution : Solution;
    Constraints : Constraints;
    Depth : int;
    Visited : Set<PackageIdentifier * PackageLocation>;
    Locations : Map<AdhocPackageIdentifier, PackageSource>;
    Hints : AsyncSeq<LocatedAtom>;
  }

  type SearchStrategy = ISourceExplorer -> SolverState -> AsyncSeq<PackageIdentifier * VersionedLocation>

  let private withTimeout timeout action =
    async {
      let! child = Async.StartChild (action, timeout)
      return! child
    }

  let constraintsOf (ds: Set<Dependency>) =
    ds
    |> Set.toSeq
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
         Constraint.satisfiesSet
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

    yield!
      state.Hints
        |> AsyncSeq.filter(fun (atom, location) -> state.Visited.Contains(atom.Package, location))
        |> AsyncSeq.map (fun (atom, location) ->
          (atom.Package, (location, atom.Versions)))


    let unsatisfied = findUnsatisfied state.Solution state.Constraints

    for package in unsatisfied do
      let acceptable =
        VersionedSource.getVersionSet
        >> (Constraint.satisfiesSet (Constraint.All (state.Constraints.[package] |> Set.toList)))
      for versionedSource in sourceExplorer.FetchVersions state.Locations package do
        if acceptable versionedSource then
          let! versionedLocation = sourceExplorer.FetchLocation versionedSource
          yield (package, versionedLocation)

  }

  let upgradeSearchStrategy (sourceExplorer : ISourceExplorer) (state : SolverState) = asyncSeq {
    let unsatisfied = findUnsatisfied state.Solution state.Constraints

    for package in unsatisfied do
      let acceptable =
        VersionedSource.getVersionSet
        >> (Constraint.satisfiesSet (Constraint.All (state.Constraints.[package] |> Set.toList)))
      for versionedSource in sourceExplorer.FetchVersions state.Locations package do
        if acceptable versionedSource then
          let! versionedLocation = sourceExplorer.FetchLocation versionedSource
          yield (package, versionedLocation)
  }

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

    if findUnsatisfied state.Solution state.Constraints |> Seq.isEmpty
      then
        yield Resolution.Ok state.Solution
      else
        let atomsToExplore =
          strategy sourceExplorer state
          |> AsyncSeq.filter (fun (package, (location, _)) ->
            Set.contains (package, location) state.Visited |> not)

        for (package, (location, versions)) in atomsToExplore do
          try
            log("Exploring " + (PackageIdentifier.show package) + " -> " + (PackageLocation.show location) + "...")

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
              Versions = versions;
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
                      Set.contains (atom.Package, location) state.Visited |> not &&
                      state.Solution.Resolutions |> Map.containsKey atom.Package |> not
                    )
                    |> AsyncSeq.ofSeq
                with error ->
                  log("Could not fetch buckaroo.lock.toml for " + (PackageLocation.show location))
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
                      |> Set.add (package, location);
                    Locations = mergedLocations;
                    Hints =
                      state.Hints
                      |> AsyncSeq.append freshHints;
                }

                yield! step context strategy nextState
              })
          with error ->
            log("Error exploring " + (PackageLocation.show location) + "...")
            log(string error)
            yield Resolution.Error error

    // We've run out of versions to try
    yield Resolution.Error (new System.Exception("No more versions to try! "))
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
