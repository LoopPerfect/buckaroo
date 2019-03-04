namespace Buckaroo

open FSharpx.Collections
open Buckaroo.Tasks
open Buckaroo.Console
open RichOutput
open FSharp.Control

module Solver =

  open FSharp.Control
  open Buckaroo.Result

  [<Literal>]
  let MaxConsecutiveFailures = 10

  type LocatedAtom = Atom * PackageLocation

  type Constraints = Map<PackageIdentifier, Set<Constraint>>

  type ResolutionPath =
  | Root of Manifest
  | Node of PackageIdentifier * ResolvedVersion

  type SolverState = {
    Locations : Map<AdhocPackageIdentifier, PackageSource>
    Root : Set<Dependency>
    Hints: Map<PackageIdentifier, List<LocatedAtom>>
    Selections : Map<PackageIdentifier, ResolvedVersion>
  }

  let constraintsOf (ds: Set<Dependency>) =
    ds
    |> Seq.map (fun x -> (x.Package, x.Constraint))
    |> Seq.groupBy fst
    |> Seq.map (fun (k, xs) -> (k, xs |> Seq.map snd |> Set.ofSeq))
    |> Map.ofSeq

  let constraintsOfSelection selections =
    Map.valueList selections
      |> List.map (fun m -> m.Manifest.Dependencies)
      |> List.fold Set.union Set.empty
      |> constraintsOf


  let pruneSelections (selections: Map<PackageIdentifier, ResolvedVersion>) (deps: Set<Dependency>) =

    let rec loop (visited: Set<PackageIdentifier>) (deps: Set<Dependency>) : seq<PackageIdentifier * ResolvedVersion> = seq {
      let notVisited =
        deps
        |> Seq.filter (fun d -> visited |> Set.contains d.Package |> not)
        |> Seq.toList

      if notVisited |> List.isEmpty
      then ()
      else
        let nextVisited = deps |> Seq.map (fun d -> d.Package) |> Set |> Set.union visited

        yield!
          notVisited
          |> Seq.filter (fun d -> selections |> Map.containsKey d.Package)
          |> Seq.map (fun d -> (d.Package, selections.[d.Package]))

        let next =
          notVisited
          |> Seq.choose (fun d -> selections |> Map.tryFind d.Package)
          |> Seq.fold (fun deps m -> Set.union m.Manifest.Dependencies deps) Set.empty

        yield! loop nextVisited next
    }

    loop Set.empty deps |> Map.ofSeq

  let isUnresolved (selections : Map<PackageIdentifier, ResolvedVersion>) (constraints : Map<PackageIdentifier, Set<Constraint>>) (dep:Dependency) =
    let c = constraints.[dep.Package] |> All |> Constraint.simplify
    selections
    |> Map.tryFind dep.Package
    |> Option.map (fun rv -> rv.Versions |> Constraint.satisfies c |> not)
    |> Option.defaultValue true

  let findUnresolved pick (selections: Map<PackageIdentifier, ResolvedVersion>) (deps: Set<Dependency>) =
    let constraints =
      Map.valueList selections
      |> List.map (fun m -> m.Manifest.Dependencies)
      |> List.fold Set.union deps
      |> constraintsOf

    let rec loop (visited: Set<PackageIdentifier>) (deps: Set<Dependency>) : seq<PackageIdentifier * Set<Constraint>> = seq {
      let notVisited =
        deps
        |> Seq.filter (fun d -> visited |> Set.contains d.Package |> not)
        |> Seq.toList

      if notVisited |> List.isEmpty
      then ()
      else
        let nextVisited = deps |> Seq.map (fun d -> d.Package) |> Set |> Set.union visited

        let next =
          notVisited
          |> Seq.choose (fun d -> selections |> Map.tryFind d.Package)
          |> Seq.fold (fun deps m -> Set.union m.Manifest.Dependencies deps) Set.empty

        yield!
          pick
            (notVisited
             |> Seq.filter (isUnresolved selections constraints)
             |> Seq.map (fun d -> (d.Package, constraints.[d.Package])))
            (loop nextVisited next)
    }

    loop Set.empty deps


  let breathFirst = findUnresolved (fun a b -> seq {
    yield! a
    yield! b
  })

  let depthFirst = findUnresolved (fun a b -> seq {
    yield! b
    yield! a
  })



  type LocatedVersionSet = PackageLocation * Set<Version>

  type PackageConstraint = PackageIdentifier * Set<Constraint>

  type SearchStrategyError =
  | LimitReached of PackageConstraint
  | Unsatisfiable of PackageConstraint
  | IntroducesConflict of List<SearchStrategyError>

  type ResolutionRequest =
  | MarkBadPath of List<ResolutionPath> * PackageConstraint * PackageConstraint * AsyncReplyChannel<Unit>
  | GetCandidates of Constraints * PackageConstraint * PackageSources * AsyncReplyChannel<AsyncSeq<Result<(PackageIdentifier * LocatedVersionSet), SearchStrategyError>>>


  type SearchStrategy = ISourceExplorer -> SolverState -> AsyncSeq<Result<PackageIdentifier * LocatedVersionSet, SearchStrategyError>>

  let fetchCandidatesForConstraint sourceExplorer locations p c = asyncSeq {
    let candidatesToExplore = SourceExplorer.fetchLocationsForConstraint sourceExplorer locations p c

    let mutable hasCandidates = false
    let mutable branchFailures = Map.empty

    for x in candidatesToExplore do
      if branchFailures |> Map.exists (fun _ v -> v > MaxConsecutiveFailures) then
        let d = (p, Set [c])
        yield
          LimitReached d
          |> Result.Error
      else
        yield!
          match x with
          | Candidate (packageLocation, c) -> asyncSeq {
              let branches =
                c
                |> Seq.choose (fun v ->
                  match v with
                  | Version.Git (Branch b) -> Some b
                  | _ -> None
                )

              try
                let! lock = sourceExplorer.LockLocation packageLocation
                do! sourceExplorer.FetchManifest (lock, c) |> Async.Ignore
                yield Result.Ok (p, (packageLocation, c))

                hasCandidates <- true

                for branch in branches do
                  branchFailures <-
                    branchFailures
                    |> Map.add branch 0

              with _ ->
                for branch in branches do
                  branchFailures <-
                    branchFailures
                    |> Map.insertWith (fun i j -> i + j + 1) branch 0
            }
          | FetchResult.Unsatisfiable (All xs) -> asyncSeq {
              let d = (p, Set xs)
              yield d |> Unsatisfiable |> Result.Error
            }
          | FetchResult.Unsatisfiable u -> asyncSeq {
              let d = (p, Set[u])
              yield d |> Unsatisfiable |> Result.Error
            }

    if hasCandidates = false
    then
      let d = (p, Set [c])
      yield
        Unsatisfiable d
        |> Result.Error
  }


  let rec constraintToSet c =
    match c with
    | All xs -> xs
    | _ -> Set [c]

  let resolutionManger (sourceExplorer : ISourceExplorer) : MailboxProcessor<ResolutionRequest> = MailboxProcessor.Start(fun inbox -> async {
    let mutable badDeps  : Map<PackageConstraint, SearchStrategyError> = Map.empty
    let mutable badCores : Set<Set<PackageConstraint>> = Set.empty
    let mutable world : Map<PackageConstraint, Set<Set<PackageConstraint>>> = Map.empty


    let testIfBad (p, cs) =
      badDeps
        |> Map.tryFindKey (fun (q, bs) _ -> p = q && cs |> Set.isSubset bs)
        |> Option.map (fun k -> badDeps.[k])

    let testIfSelectionGood (constraints : Constraints) =
      constraints
      |> Map.toSeq
      |> Seq.tryFind (fun (p, cs) ->
        badDeps |> Map.containsKey (p, cs))
      |> Option.isNone

    let testIfHasBadCore (constraints : Constraints) =
      let deps =
        constraints
        |> Map.toSeq
        |> Set

      let isBad =
        badCores
        |> Set.exists (fun core -> Set.isSuperset deps core)

      System.Console.WriteLine (badCores
        |> Set.map (fun core -> Set.difference core deps))

      if isBad
      then System.Console.WriteLine "BADDDDDDDDDDDDDDDDDDDDDDDDDDDDDDddDDDDDDDDDDDDDDDDDDD"

      isBad


    let trackLocal locations (p, cs) = asyncSeq {
      let mutable hadCandidate = false
      let c = cs |>  All |> Constraint.simplify

      let isBad = testIfBad (p, cs)

      match isBad with
      | Some e -> yield Result.Error e
      | None ->
        for candidate in fetchCandidatesForConstraint sourceExplorer locations p c do
          match candidate with
          | Result.Error (IntroducesConflict _) -> ()
          | Result.Error (Unsatisfiable d) ->
            badDeps <- (badDeps |> Map.add d (Unsatisfiable d))
            yield Result.Error <| Unsatisfiable d
          | Result.Error (LimitReached d) ->
            if hadCandidate <> false
            then
              badDeps <- (badDeps |> Map.add d (LimitReached d))
            yield Result.Error <| LimitReached d
          | Result.Ok (_, (location, versions)) ->
            let! lock = sourceExplorer.LockLocation location
            let! manifest = sourceExplorer.FetchManifest (lock, versions)
            let packageConstraints = manifest.Dependencies |> Set.map (fun d -> (d.Package, d.Constraint |> constraintToSet))

            world <- (world |> Map.insertWith Set.union (p, cs) (Set [packageConstraints]))

            let conflicts =
              manifest.Dependencies
              |> Seq.choose (fun d -> badDeps |> Map.tryFind (d.Package, constraintToSet d.Constraint))
              |> Seq.toList

            if conflicts.IsEmpty
            then
              hadCandidate <- true
              yield candidate
            else
              ()
              //ield Result.Error (IntroducesConflict conflicts)

        if hadCandidate
        then ()
        else ()
          //badDeps <- (badDeps |> Map.add (p, cs) (IntroducesConflict[]))


    }

    let trackGlobal (constraints: Constraints) (candidates: AsyncSeq<Result<PackageIdentifier * LocatedVersionSet, SearchStrategyError>>) = asyncSeq {
      for candidate in candidates do
        match candidate with
        | Result.Error e ->
          yield Result.Error e
        | Result.Ok (_, (location, versions)) ->
          let! lock = sourceExplorer.LockLocation location
          let! manifest = sourceExplorer.FetchManifest (lock, versions)
          let conflicts =
            manifest.Dependencies
            |> Seq.filter (fun d -> constraints |> Map.containsKey d.Package)
            |> Seq.map (fun d -> (d.Package, constraintToSet d.Constraint |> Set.union constraints.[d.Package]))
            |> Seq.choose (fun (p, _) ->
              badDeps
              |> Map.tryFindKey (fun (q, bs) _ -> p = q && constraints.[p] |> Set.isSubset bs)
              |> Option.map (fun k -> badDeps.[k]))
            |> Seq.toList

          if conflicts.IsEmpty
          then
            yield candidate
          else
            yield Result.Error (IntroducesConflict conflicts)
        ()

      ()
    }

    while true do
      let! req = inbox.Receive()
      match req with
      | GetCandidates (constraints, dep, locations, channel) ->
        trackLocal locations dep
        |> trackGlobal constraints
        |> AsyncSeq.takeWhile (fun _ -> testIfBad dep |> Option.isNone)
        |> AsyncSeq.takeWhile (fun _ -> testIfSelectionGood constraints)
        |> AsyncSeq.takeWhile (fun _ -> testIfHasBadCore constraints |> not)
        |> channel.Reply
      | MarkBadPath (path, failedDep, (p, bs), channel) ->
        //System.Console.WriteLine ("Marking " + string failedDep + " because " + string (p, bs) )

        let groups =
          world.[failedDep]
          |> Set.map(fun xs ->
            xs
            |> Set.filter(fun (q, _) -> p = q )
            |> Set.map(fun (_, cs) -> cs )
            |> Set.unionMany)

        for contribution in groups do

          let core =
            path
            |> Seq.choose(fun x ->
              match x with
              | Root m -> Some m.Dependencies
              | Node (q, rv) ->
                if q <> fst failedDep && p <> q
                then
                  Some rv.Manifest.Dependencies
                else None)
            |> Seq.map (fun deps ->
              deps
              |> Seq.map (fun x -> (x.Package, x.Constraint |> constraintToSet |> (fun c -> Set.difference c contribution)))
              |> Seq.filter (fun (q, cs) -> cs.IsEmpty |> not)
              |> Seq.filter (fun (q, cs) -> p = q && Set.isProperSubset cs bs) // should be an intersection
              |> Set

            )
            |> Set.unionMany
            |> Set.add failedDep

          badCores <- badCores |> Set.add core
         // System.Console.WriteLine "bad core: "
          //System.Console.WriteLine core
         // System.Console.WriteLine "-------"

        channel.Reply ()

  })


  let rec private step (context : TaskContext) (resolver : MailboxProcessor<ResolutionRequest>) (state : SolverState) (path: List<ResolutionPath>): AsyncSeq<SolverState> = asyncSeq {
    let sourceExplorer = context.SourceExplorer
    let log = namespacedLogger context.Console ("solver")

    let selections = pruneSelections state.Selections state.Root
    let constraints =
      selections
      |> Map.valueList
      |> Seq.map (fun m -> m.Manifest.Dependencies)
      |> Seq.append [state.Root]
      |> Set.unionMany
      |> constraintsOf


    let manifests =
      selections
      |> Map.valueList
      |> Seq.map (fun rv -> rv.Manifest)
      |> Seq.toList

    let locations =
      manifests
      |> Seq.map (fun m -> m.Locations |> Map.toSeq)
      |> Seq.fold Seq.append (state.Locations |> Map.toSeq)
      |> Map.ofSeq

    let unresolved = depthFirst selections state.Root |> Seq.toList

    if (unresolved |> Seq.isEmpty)
    then
      yield state
    else
      for (p, cs) in unresolved do
        let c = cs |> All

        let hints =
          state.Hints
          |> Map.tryFind p
          |> Option.defaultValue([])
          |> Seq.filter(fun (atom, _) -> atom.Versions |> Constraint.satisfies c)
          |> AsyncSeq.ofSeq
          |> AsyncSeq.chooseAsync(fun (atom, location) -> async {
            try
              let! lock = sourceExplorer.LockLocation location
              let! manifest = sourceExplorer.FetchManifest (lock, atom.Versions)
              let resolvedVersion = {
                Lock = lock
                Versions = atom.Versions
                Manifest = manifest
              }
              return Some {state with Selections = state.Selections |> Map.add p resolvedVersion}
            with _ -> return None
          })

        let! requested =
          resolver.PostAndAsyncReply (fun channel -> GetCandidates (constraints, (p, cs), locations, channel))
        let candidates = requested |> AsyncSeq.cache

        let fetched =
          candidates
          |> AsyncSeq.chooseAsync(fun candidate -> async {
            match candidate with
            | Result.Error e ->
              //System.Console.WriteLine e
              return None
            | Result.Ok (_, (location, versions)) ->
              let! lock = sourceExplorer.LockLocation location
              let! manifest = sourceExplorer.FetchManifest (lock, versions)
              let resolvedVersion = {
                Lock = lock
                Versions = versions
                Manifest = manifest
              }
              return Some {state with Selections = state.Selections |> Map.add p resolvedVersion}})
              |> AsyncSeq.distinctUntilChangedWith (fun prev next ->
                  prev.Selections.[p].Manifest = next.Selections.[p].Manifest
              )

        for nextState in AsyncSeq.append hints fetched do
          let node = Node (p, nextState.Selections.[p])
          let visited = path |> List.contains node
          if visited <> true
          then
            yield! step context resolver nextState (node :: path)


        let! error =
          candidates
          |> AsyncSeq.choose (fun candidate ->
            match candidate with
            | Result.Error (IntroducesConflict [Unsatisfiable (p, cs)]) ->
              //System.Console.WriteLine "####################################################################"
              Some (p, cs)
            | _ -> None)
          |> AsyncSeq.tryFirst

        match error with
        | Some transitiveFailure ->
          ()
          do! resolver.PostAndAsyncReply (fun ch -> MarkBadPath (path, (p, cs), transitiveFailure, ch))

          constraints
          |> Map.toSeq
          |> Seq.map(fun x -> System.Console.WriteLine x)
          |> Seq.toList
          |> ignore


        | None -> ()



    // System.Console.WriteLine (unresolved |> List.map (fun (p, cs) -> string p + (string cs) ) |> String.concat "\n")

    // System.Console.WriteLine (
    //   path
    //   |> List.map(
    //     fun x ->
    //       match x with
    //       | Root _ -> "Root"
    //       | Node (p, r) -> (string p + "@" + string r.Versions))
    //   |> String.concat "\n"
    // )
    // ()
  }

  let solutionCollector resolutions =
    resolutions
    |> AsyncSeq.take (1024)
    |> AsyncSeq.takeWhileInclusive (fun x ->
      match x with
      | Backtrack _ -> false
      | _ -> true)
    |> AsyncSeq.filter (fun x ->
      match x with
      | Ok _ -> true
      | Backtrack _ -> true
      | _ -> false)
    |> AsyncSeq.take 1
    |> AsyncSeq.toListAsync
    |> Async.RunSynchronously
    |> List.tryHead

  let solve (context : TaskContext) (partialSolution : Solution) (manifest : Manifest) (style : ResolutionStyle) (lock : Lock option) = async {
    let hints = Map.empty
    //  lock
    //  |> Option.map (fun l ->
    //    l.Packages |> Map.map (fun p v -> [({Package = p; Versions = v.Versions}, v.Location)] )   )
    //  |> Option.defaultValue Map.empty

    let state = {
      Root = Set.union
        manifest.Dependencies
        manifest.PrivateDependencies
      Hints = hints
      Selections = Map.empty
      Locations = manifest.Locations
    }

    let resolver = resolutionManger context.SourceExplorer

    let resolutions =
      step context resolver state [Root manifest]

    let result =
      resolutions
      |> AsyncSeq.map (fun s ->
        Resolution.Ok <| {Resolutions = s.Selections |> Map.map(fun k v -> (v, Solution.empty))}
      )
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
