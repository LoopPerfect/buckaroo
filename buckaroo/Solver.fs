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

  type PackageConstraint = PackageIdentifier * Set<Constraint>


  type LocatedVersionSet = PackageLocation * Set<Version>

  type SearchStrategyError =
  | LimitReached of PackageConstraint
  | Unresolvable of PackageConstraint
  | TransitiveConflict of Set<PackageConstraint> * SearchStrategyError
  | Conflicts of Set<SearchStrategyError>

  type ResolutionRequest =
  | MarkBadPath of List<ResolutionPath> * PackageConstraint * Set<SearchStrategyError> * AsyncReplyChannel<Unit>
  | GetCandidates of Constraints * PackageConstraint * PackageSources * AsyncReplyChannel<AsyncSeq<Result<(PackageIdentifier * LocatedVersionSet), SearchStrategyError>>>

  type SearchStrategy = ISourceExplorer -> SolverState -> AsyncSeq<Result<PackageIdentifier * LocatedVersionSet, SearchStrategyError>>


  let toDnf c =
    match c with
    | All xs -> xs
    | _ -> Set [c]

  let toPackageConstraint (dep : Dependency) : PackageConstraint =
    (dep.Package, toDnf dep.Constraint)

  let constraintsOf (ds: seq<PackageConstraint>) =
    ds
    |> Seq.groupBy fst
    |> Seq.map (fun (k, xs) -> (k, xs |> Seq.map snd |> Set.unionMany))
    |> Map.ofSeq

  let constraintsOfSelection selections =
    Map.valueList selections
      |> List.map (fun m -> m.Manifest.Dependencies)
      |> List.map (Set.map toPackageConstraint)
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

  let isUnresolved (selections : Map<PackageIdentifier, ResolvedVersion>) (constraints : Map<PackageIdentifier, Set<Constraint>>) (dep : Dependency) =
    let c = constraints.[dep.Package] |> All |> Constraint.simplify
    selections
    |> Map.tryFind dep.Package
    |> Option.map (fun rv -> rv.Versions |> Constraint.satisfies c |> not)
    |> Option.defaultValue true

  let findUnresolved pick (selections: Map<PackageIdentifier, ResolvedVersion>) (deps: Set<Dependency>) =
    let constraints =
      Map.valueList selections
      |> List.map (fun m -> m.Manifest.Dependencies)
      |> List.map (Set.map toPackageConstraint)
      |> List.fold Set.union (deps |> Set.map toPackageConstraint)
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
              yield d |> Unresolvable |> Result.Error
            }
          | FetchResult.Unsatisfiable u -> asyncSeq {
              let d = (p, Set[u])
              yield d |> Unresolvable |> Result.Error
            }

    if hasCandidates = false
    then
      let d = (p, Set [c])
      yield
        Unresolvable d
        |> Result.Error
  }


  let resolutionManger (sourceExplorer : ISourceExplorer) : MailboxProcessor<ResolutionRequest> = MailboxProcessor.Start(fun inbox -> async {
    let mutable unresolvableCores : Map<Set<PackageConstraint>, SearchStrategyError> = Map.empty
    let mutable underconstraintDeps : Set<PackageConstraint> = Set.empty
    let mutable world : Map<PackageConstraint, Set<Set<PackageConstraint>>> = Map.empty

    let testIfHasBadCore (constraints : Constraints) =
      let deps =
        constraints
        |> Map.toSeq
        |> Set

      unresolvableCores
        |> Map.toSeq
        |> Seq.filter (fun (core, _) ->
          core
          |> Set.forall (fun (p, bs) ->
            constraints
            |> Map.tryFind p
            |> Option.map (Set.isSubset bs)
            |> Option.defaultValue false
        ))


    let trackLocal locations (p, cs) = asyncSeq {
      let mutable hadCandidate = false
      let c = cs |>  All |> Constraint.simplify

      let conflicts = testIfHasBadCore (Map.ofSeq [(p, cs)]) |> Seq.tryHead

      match conflicts with
      | Some (dep, _) ->
        System.Console.WriteLine (string (p, cs))
        yield Result.Error (Unresolvable dep.MinimumElement)
      | None ->
        for candidate in fetchCandidatesForConstraint sourceExplorer locations p c do
          match candidate with
          | Result.Error (Unresolvable d) ->
            unresolvableCores <- (unresolvableCores |> Map.add (Set [d]) (Unresolvable d))
            yield Result.Error <| Unresolvable d
          | Result.Error (LimitReached d) ->
            if hadCandidate <> false
            then
              underconstraintDeps <- (underconstraintDeps |> Set.add d)
            yield Result.Error <| LimitReached d
          | Result.Ok (_, (location, versions)) ->
            let! lock = sourceExplorer.LockLocation location
            let! manifest = sourceExplorer.FetchManifest (lock, versions)
            let packageConstraints =
              manifest.Dependencies
              |> Set.map (fun d -> (d.Package, d.Constraint |> toDnf))

            world <- (world |> Map.insertWith Set.union (p, cs) (Set [packageConstraints]))

            let conflicts =
              manifest.Dependencies
              |> Set.map toPackageConstraint
              |> constraintsOf
              |> Map.insertWith Set.union p cs
              |> testIfHasBadCore
              |> Seq.map TransitiveConflict
              |> Set


            if conflicts |> Set.isEmpty
            then
              hadCandidate <- true
              yield candidate
            else
              System.Console.WriteLine "foo"
              yield Result.Error (Conflicts conflicts)
          | _ -> ()
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
            |> Seq.map toPackageConstraint
            |> constraintsOf
            |> testIfHasBadCore
            |> Seq.map TransitiveConflict
            |> Set

          if conflicts |> Set.isEmpty
          then
            yield candidate
          else
            System.Console.WriteLine "bar"
            yield Result.Error (Conflicts conflicts)
        ()

      ()
    }

    while true do
      let! req = inbox.Receive()
      match req with
      | GetCandidates (constraints, dep, locations, channel) ->
        trackLocal locations dep
        |> AsyncSeq.takeWhile (fun _ -> testIfHasBadCore constraints |> Seq.isEmpty)
        |> trackGlobal constraints
        |> channel.Reply
      | MarkBadPath (path, failedDep, errors, channel) ->


        let rec compute error =

          match error with
          | LimitReached _-> () // TODO
          | Unresolvable (p, bs) ->
            System.Console.WriteLine "unresolvable..."

            if failedDep <> (p, bs)
            then
              let groups =
                world.[failedDep]
                |> Set.map(fun xs ->
                  xs
                  |> Set.filter(fun (q, _) -> p = q )
                  |> Set.map(fun (_, cs) -> cs )
                  |> Set.unionMany)

              System.Console.WriteLine "xxx"
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
                    |> Seq.map (fun x -> (x.Package, x.Constraint |> toDnf |> (fun c -> Set.difference c contribution)))
                    |> Seq.filter (fun (q, cs) -> cs.IsEmpty |> not)
                    |> Seq.filter (fun (q, cs) -> p = q && Set.isProperSubset cs bs) // should be an intersection?
                    |> Set)
                  |> Set.unionMany
                  |> Set.add failedDep
                unresolvableCores <- unresolvableCores |> Map.add core (SearchStrategyError.Unresolvable (p, bs))
              else
                unresolvableCores <- unresolvableCores |> Map.add (Set[(p, bs)]) (SearchStrategyError.Unresolvable (p, bs))
          | TransitiveConflict (core, next) ->
            System.Console.WriteLine (string (core, next))
            compute next
          | Conflicts cs ->
            for c in cs do compute c

        for error in errors do
          System.Console.WriteLine "errors"
          compute error
        channel.Reply ()
        System.Console.WriteLine "done"


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
      |> Seq.map (Set.map toPackageConstraint)
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
          |> AsyncSeq.mapAsync(fun candidate -> async {
            match candidate with
            | Result.Error e ->
              return Result.Error e
            | Result.Ok (_, (location, versions)) ->
              let! lock = sourceExplorer.LockLocation location
              let! manifest = sourceExplorer.FetchManifest (lock, versions)
              let resolvedVersion = {
                Lock = lock
                Versions = versions
                Manifest = manifest
              }
              return Result.Ok {state with Selections = state.Selections |> Map.add p resolvedVersion}})
          |> AsyncSeq.distinctUntilChangedWith (fun prev next ->
            match prev, next with
            | (Result.Ok prevS), (Result.Ok nextS) ->
              prevS.Selections.[p].Manifest = nextS.Selections.[p].Manifest
            | (_, _) -> prev = next)


        for nextState in AsyncSeq.append hints (fetched |> AsyncSeq.choose (fun x -> match x with | Result.Ok v -> Some v | _ -> None)) do
          let node = Node (p, nextState.Selections.[p])
          let visited = path |> List.contains node
          if visited <> true
          then
            yield! step context resolver nextState (node :: path)

        let errors =
          candidates
          |> AsyncSeq.choose (fun candidate ->
            match candidate with
            | Result.Error e ->
              Some e
            | _ -> None)
          |> AsyncSeq.toBlockingSeq
          |> Set

        do! resolver.PostAndAsyncReply (fun ch -> MarkBadPath (path, (p, cs), errors, ch))

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
