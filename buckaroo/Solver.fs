module Buckaroo.Solver

open FSharp.Control
open FSharpx
open FSharpx.Collections
open Buckaroo.Tasks
open Buckaroo.Console
open Buckaroo.RichOutput
open Buckaroo.Logger
open Buckaroo.Constraint
open Buckaroo.SearchStrategy
open Buckaroo.Prefetch

type LocatedAtom = Atom * PackageLock

type Constraints = Map<PackageIdentifier, Set<Constraint>>

type ResolutionPath =
| Root of Manifest
| Node of PackageIdentifier * Set<Constraint> * ResolvedVersion

type SolverState =
  {
    Locations : Map<AdhocPackageIdentifier, PackageSource>
    Root : Set<Dependency>
    Selections : Map<PackageIdentifier, (ResolvedVersion * Solution)>
    Hints : Map<PackageIdentifier, List<LockedPackage>>
  }

type ResolutionRequest =
| MarkBadPath of List<ResolutionPath> * PackageConstraint * SearchStrategyError * AsyncReplyChannel<Unit>
| ProposeCandidates of Constraints * PackageConstraint * seq<LockedPackage> * AsyncReplyChannel<AsyncSeq<Result<ResolvedVersion, SearchStrategyError>>>
| GetCandidates of Constraints * PackageConstraint * PackageSources * AsyncReplyChannel<AsyncSeq<Result<(PackageIdentifier * LocatedVersionSet), SearchStrategyError>>>

let private ifError x =
  match x with
  | Result.Error e -> Some e
  | _ -> None

let private ifOk x =
  match x with
  | Result.Ok v -> Some v
  | _ -> None

let private resultOrDefaultWith f x =
  match x with
  | Result.Ok v -> v
  | Result.Error e -> f e

let toDnf c =
  let d = simplify c
  match d with
  | All xs -> xs
  | _ -> Set [ d ]

let toPackageConstraint (dep : Dependency) : PackageConstraint =
  (dep.Package, toDnf dep.Constraint)

let constraintsOf (ds: seq<PackageConstraint>) =
  ds
  |> Seq.groupBy fst
  |> Seq.map (fun (k, xs) -> (k, xs |> Seq.map snd |> Set.unionMany))
  |> Map.ofSeq

let constraintsOfSelection selections =
  Map.valueList selections
  |> Seq.map (fun m ->
    m.Manifest.Dependencies
    |> Set.map toPackageConstraint
  )
  |> Seq.fold Set.union Set.empty
  |> constraintsOf

let pruneSelections (selections: Map<PackageIdentifier, ResolvedVersion * Solution>) (deps: Set<Dependency>) =
  let rec loop (visited: Set<PackageIdentifier>) (deps: Set<Dependency>) : seq<PackageIdentifier * (ResolvedVersion * Solution)> = seq {
    let notVisited =
      deps
      |> Seq.filter (fun d -> visited |> Set.contains d.Package |> not)
      |> Seq.toList

    if notVisited |> List.isEmpty
    then ()
    else
      let nextVisited =
        deps
        |> Seq.map (fun d -> d.Package)
        |> Set
        |> Set.union visited

      yield!
        notVisited
        |> Seq.filter (fun d -> selections |> Map.containsKey d.Package)
        |> Seq.map (fun d -> (d.Package, selections.[d.Package]))

      let next =
        notVisited
        |> Seq.choose (fun d -> selections |> Map.tryFind d.Package)
        |> Seq.fold (fun deps (rv, _) -> Set.union rv.Manifest.Dependencies deps) Set.empty

      yield! loop nextVisited next
  }

  loop Set.empty deps |> Map.ofSeq

let isUnresolved (selections : Map<PackageIdentifier, ResolvedVersion * Solution>) (constraints : Map<PackageIdentifier, Set<Constraint>>) (dep : Dependency) =
  let c = constraints.[dep.Package] |> All |> Constraint.simplify
  selections
  |> Map.tryFind dep.Package
  |> Option.map fst
  |> Option.map (fun rv -> rv.Versions |> Constraint.satisfies c |> not)
  |> Option.defaultValue true

let findUnresolved pick (selections: Map<PackageIdentifier, ResolvedVersion * Solution>) (deps: Set<Dependency>) =
  let constraints =
    Map.valueList selections
    |> Seq.map fst
    |> Seq.map (fun m -> m.Manifest.Dependencies)
    |> Seq.map (Set.map toPackageConstraint)
    |> Seq.fold Set.union (deps |> Set.map toPackageConstraint)
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
        |> Seq.map fst
        |> Seq.fold (fun deps m -> Set.union m.Manifest.Dependencies deps) Set.empty

      yield!
        pick
          (notVisited
           |> Seq.filter (isUnresolved selections constraints)
           |> Seq.map (fun d -> (d.Package, constraints.[d.Package])))
          (loop nextVisited next)
  }

  loop Set.empty deps


let breadthFirst = findUnresolved (fun a b -> seq {
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
    if branchFailures |> Map.exists (fun _ v -> v > Constants.MaxConsecutiveFailures) then
      let d = (p, Set [ c ])
      yield
        LimitReached (d, Constants.MaxConsecutiveFailures)
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

  if not hasCandidates
  then
    let d = (p, Set [c])
    yield
      Unresolvable d
      |> Result.Error
}

let resolutionManager (sourceExplorer : ISourceExplorer) (logger : Logger) : MailboxProcessor<ResolutionRequest> =
  MailboxProcessor.Start(fun inbox -> async {
    let mutable unresolvableCores : Map<Set<PackageConstraint>, SearchStrategyError> = Map.empty
    let mutable underconstraintDeps : Set<PackageConstraint> = Set.empty
    let mutable world : Map<PackageConstraint, Set<Set<PackageConstraint>>> = Map.empty

    let findBadCores (constraints : Constraints) =
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

    let trackLocal locations (p, cs) constraintsContext = asyncSeq {
      let mutable hadCandidate = false
      let c = cs |> All |> Constraint.simplify

      let conflicts = findBadCores (Map.ofSeq [ (p, cs) ]) |> Seq.tryHead

      match conflicts with
      | Some (dep, _) ->
        yield Result.Error (Unresolvable dep.MinimumElement)
      | None ->
        let candidates =
          fetchCandidatesForConstraint sourceExplorer locations p c
          |> AsyncSeq.takeWhile (fun _ ->
            findBadCores constraintsContext |> Seq.isEmpty
          )

        for candidate in candidates do
          match candidate with
          | Result.Error (Unresolvable d) ->
            unresolvableCores <- (unresolvableCores |> Map.add (Set [d]) (Unresolvable d))
            let (p, cs) = d
            logger.RichWarning (
              "Unresolvable: " +
              PackageIdentifier.showRich p +
              subtle "@" +
              (highlight <| Constraint.show (All cs |> simplify))
            )
            yield Result.Error <| Unresolvable d
          | Result.Error (LimitReached (d, Constants.MaxConsecutiveFailures)) ->
            if hadCandidate && (Set.contains d underconstraintDeps |> not)
            then
              underconstraintDeps <- (underconstraintDeps |> Set.add d)
              let (p, cs) = d
              logger.RichWarning (
                text("No manifest found for: ") +
                PackageIdentifier.showRich p +
                subtle "@" +
                Constraint.show (All cs)
              )
              logger.Warning ("... is this a valid Buckaroo package?")

            yield Result.Error <| LimitReached (d, Constants.MaxConsecutiveFailures)
          | Result.Ok (_, (location, versions)) ->
            let! lock = sourceExplorer.LockLocation location
            let! manifest =
              sourceExplorer.FetchManifest (lock, versions)

            let packageConstraints =
              manifest.Dependencies
              |> Set.map (fun d -> (d.Package, d.Constraint |> toDnf))

            world <- (
              world
              |> Map.insertWith Set.union (p, cs) (Set [packageConstraints])
            )

            let conflicts =
              manifest.Dependencies
              |> Set.map toPackageConstraint
              |> constraintsOf
              |> Map.insertWith Set.union p cs
              |> findBadCores
              |> Set

            if conflicts |> Set.isEmpty
            then
              hadCandidate <- true
              yield candidate
            else
              yield Result.Error (TransitiveConflict conflicts)
          | _ -> ()


    }

    let trackGlobal (constraints : Constraints) (candidates : AsyncSeq<Result<PackageIdentifier * LocatedVersionSet, SearchStrategyError>>) =
      asyncSeq {
        for candidate in candidates do
          match candidate with
          | Result.Error e ->
            yield Result.Error e
          | Result.Ok (_, (location, versions)) ->
            let! lock = sourceExplorer.LockLocation location
            let! manifest =
              sourceExplorer.FetchManifest (lock, versions)

            let conflicts =
              manifest.Dependencies
              |> Seq.map toPackageConstraint
              |> Seq.append (constraints |> Map.toSeq)
              |> constraintsOf
              |> findBadCores
              |> Set

            if Set.isEmpty conflicts
            then
              yield candidate
            else
              yield Result.Error (TransitiveConflict conflicts)
          ()
        ()
      }

    let depsFromPath p =
      match p with
      | Root m -> Set.union m.Dependencies m.PrivateDependencies
      | Node (_, _, rv) -> rv.Manifest.Dependencies

    while true do
      let! req = inbox.Receive ()
      match req with
      | ProposeCandidates (constraints, (p, cs), lockedPackages, channel) ->
        lockedPackages
        |> AsyncSeq.ofSeq
        |> AsyncSeq.mapAsync(fun lp -> async {
          try
            let! manifest =
              sourceExplorer.FetchManifest (lp.Location, lp.Versions)

            let conflicts =
              manifest.Dependencies
              |> Set.map toPackageConstraint
              |> constraintsOf
              |> Map.insertWith Set.union p cs
              |> findBadCores
              |> Seq.map (Set.singleton >> TransitiveConflict)

            if conflicts |> Seq.isEmpty |> not
            then
              return Result.Error (NoManifest p) // TODO ...
            else
              let rv : ResolvedVersion =
                {
                  Manifest = manifest
                  Lock = lp.Location
                  Versions = lp.Versions
                }

              return Result.Ok rv
          with _ ->
            return Result.Error (NoManifest p)
        })
        |> AsyncSeq.filter (fun x ->
          match x with
          | Result.Error _ -> false
          | _ -> true
        )
        |> AsyncSeq.takeWhile (fun _ ->
          findBadCores constraints |> Seq.isEmpty
        )
        |> channel.Reply
      | GetCandidates (constraints, dep, locations, channel) ->
        trackLocal locations dep constraints
        |> trackGlobal constraints
        |> channel.Reply
      | MarkBadPath (path, failedDep, error, channel) ->
          match error with
          | TransitiveConflict conflicts ->
            let unresolvables =
              conflicts
              |> Seq.choose (fun (_, e) ->
                match e with
                | Unresolvable (p, bs) -> Some (p, bs)
                | _ -> None
              )

            for (p, bs) in unresolvables do
              let contributions =
                match world |> Map.tryFind failedDep with
                | None ->
                  Set.empty
                | Some buckets ->
                  buckets
                  |> Set.map(fun deps ->
                      deps
                      |> Seq.filter (fun (q, _) -> p = q)
                      |> Seq.map (fun (_, cs) -> cs)
                      |> Set.unionMany)

              for contrib in contributions do
                let core =
                  path
                  |> Seq.filter (fun x ->
                    match x with
                    | Node (q, _, _) -> p <> q
                    | _ -> true)
                  |> Seq.map (depsFromPath >>
                    (
                      fun deps ->
                        deps
                        |> Seq.map (fun x -> (x.Package, x.Constraint |> toDnf))
                        |> Seq.filter (fun (q, cs) -> p = q && cs <> contrib)
                        |> Seq.map (fun (q, cs) -> (q, Set.intersect cs bs))
                        |> Seq.map (fun (q, cs) -> (q, Set.difference cs contrib))
                        |> Seq.filter (fun (_, cs) -> cs.IsEmpty |> not)
                        |> Set
                    )
                  )
                  |> Set.unionMany
                  |> Set.add failedDep

                unresolvableCores <-
                  unresolvableCores
                  |> Map.add core (SearchStrategyError.Unresolvable (p, bs))
          | _ -> ()


          channel.Reply ()
  })

let getHints (resolver: MailboxProcessor<ResolutionRequest>) state selections p cs = asyncSeq {

  let constraints =
    selections
    |> Map.valueList
    |> Seq.map (fst >> (fun m -> m.Manifest.Dependencies))
    |> Seq.append [ state.Root ]
    |> Seq.map (Set.map toPackageConstraint)
    |> Set.unionMany
    |> constraintsOf

  let c = All cs
  let candidates =
    state.Hints
    |> Map.tryFind p
    |> Option.defaultValue []
    |> Seq.filter (fun lp -> lp.Versions |> Constraint.satisfies c)
    |> Seq.distinct

  let! request =
    resolver.PostAndAsyncReply
      (fun channel ->
        ProposeCandidates (constraints, (p, cs), candidates, channel))

  yield! request
}

let fetchHints (sourceExplorer : ISourceExplorer) (state: SolverState) (resolvedVersion : ResolvedVersion) : Async<SolverState> = async {
  try
    let! lock =
      sourceExplorer.FetchLock
        (resolvedVersion.Lock, resolvedVersion.Versions)

    let hints =
      Seq.append
        (state.Hints |> Map.toSeq)
        (lock.Packages
          |> Map.toSeq
          |> Seq.map (fun (k, v) -> (k, [v])))
      |> Seq.groupBy fst
      |> Seq.map (fun (k, vs) -> (k, vs |> Seq.map snd |> Seq.distinct |> List.concat))
      |> Map.ofSeq

    return {
      state with Hints = hints
    }
  with _ ->
    return state
}

let collectPrivateHints (state : SolverState) (p : PackageIdentifier) =
  state.Hints
  |> Map.tryFind p
  |> Option.defaultValue []
  |> Seq.map (fun l -> l.PrivatePackages |> Map.toSeq)
  |> Seq.collect id
  |> Seq.groupBy fst
  |> Seq.map (fun (k, vs) -> (k, vs |> Seq.map snd |> Seq.distinct |> Seq.map List.singleton |> List.concat))
  |> Map.ofSeq

let getCandidates (resolver: MailboxProcessor<ResolutionRequest>) (sourceExplorer: ISourceExplorer) state selections p cs = asyncSeq {
    let constraints =
      selections
      |> Map.valueList
      |> Seq.map (fst >> (fun m -> m.Manifest.Dependencies))
      |> Seq.append [ state.Root ]
      |> Seq.map (Set.map toPackageConstraint)
      |> Set.unionMany
      |> constraintsOf

    let manifests =
      selections
      |> Map.valueList
      |> Seq.map (fst >> (fun rv -> rv.Manifest))
      |> Seq.toList

    let locations =
      manifests
      |> Seq.map (fun m -> m.Locations |> Map.toSeq)
      |> Seq.fold Seq.append (state.Locations |> Map.toSeq)
      |> Map.ofSeq

    let! requested =
      resolver.PostAndAsyncReply
        (fun channel ->
          GetCandidates (constraints, (p, cs), locations, channel))

    yield!
      requested
      |> AsyncSeq.mapAsync (fun candidate -> async {
        match candidate with
        | Result.Error e ->
          return Result.Error e
        | Result.Ok (_, (location, versions)) ->
          let! lock = sourceExplorer.LockLocation location
          let! manifest =
            sourceExplorer.FetchManifest (lock, versions)

          let resolvedVersion = {
            Lock = lock
            Versions = versions
            Manifest = manifest
          }

          return Result.Ok resolvedVersion
      })
      |> AsyncSeq.distinctUntilChangedWith (fun prev next ->
        match prev, next with
        | (Result.Ok p), (Result.Ok n) ->
          p.Manifest = n.Manifest // All revisions with an identical manifest will have the same outcome
        | (_, _) -> prev = next
      )
}

let zipState state clause =
  Result.map (fun candidate -> (clause, state, candidate))
  >> Result.mapError(fun e -> (clause, e))

let mergeHint sourceExplorer next = async {
  match next with
  | Result.Ok (clause, state, rv) ->
    let! nextState = fetchHints sourceExplorer state rv
    return Result.Ok (clause, nextState, rv)
  | Result.Error e -> return Result.Error e
}

let quickStrategy resolver sourceExplorer state selections = asyncSeq {
  let unresolved =
    breadthFirst selections state.Root
    |> Seq.sortByDescending (snd >> All >> simplify >> Constraint.chanceOfSuccess)

  for (p, cs) in unresolved do
    yield!
      (AsyncSeq.append
        (getHints resolver state selections p cs)
        (getCandidates resolver sourceExplorer state selections p cs))
      |> AsyncSeq.map (zipState state (p, cs))
      |> AsyncSeq.mapAsync (mergeHint sourceExplorer)
}

let upgradeStrategy resolver sourceExplorer state selections = asyncSeq {
  let unresolved =
    breadthFirst selections state.Root
    |> Seq.sortByDescending (snd >> All >> simplify >> Constraint.chanceOfSuccess)

  for (p, cs) in unresolved do
    yield!
      getCandidates resolver sourceExplorer state selections p cs
      |> AsyncSeq.map (zipState state (p, cs))
}

let private privateStep step ((p, _), state, rv) =
  let manifest = rv.Manifest
  let privateState : SolverState =
    {
      state with
        Hints = collectPrivateHints state p
        Root = manifest.PrivateDependencies
        Locations = state.Locations
        Selections = Map.empty
    }
  (step privateState [ Root manifest ])
    |> AsyncSeq.choose ifOk
    |> AsyncSeq.tryFirst

let rec private step (context : TaskContext) (resolver : MailboxProcessor<ResolutionRequest>) (prefetcher : Prefetcher) strategy (state : SolverState) (path: List<ResolutionPath>): AsyncSeq<Result<Solution, PackageConstraint * SearchStrategyError>> = asyncSeq {
  let logger = createLogger context.Console (Some "solver")
  let nextStep = step context resolver prefetcher strategy

  let selections = pruneSelections state.Selections state.Root

  if breadthFirst selections state.Root |> Seq.isEmpty
  then
    yield Result.Ok { Resolutions = selections }
  else
    let results =
      asyncSeq {
        let xs : AsyncSeq<Result<_, _>> = strategy state selections

        for x in xs do
          match x with
          | Result.Ok ((p, cs), state, rv) ->
            if path |> List.contains (Node (p, cs, rv)) |> not
            then
              logger.Info (
                "Trying " + (PackageIdentifier.show p) + " at " +
                (rv.Versions |> Seq.map Version.show |> String.concat ", "))

              for p in rv.Manifest.Dependencies |> Seq.map (fun d -> d.Package) |> Seq.distinct do
                prefetcher.Prefetch p

              let! privateSolution = privateStep nextStep ((p, cs), state, rv)

              match privateSolution with
              | None ->
                yield Result.Error ((p, cs), NoPrivateSolution p) // TODO: propagate error
              | Some ps ->
                let node = Node (p, cs, rv)
                let nextState =
                  {
                    state with
                      Selections = selections |> Map.add p (rv, ps)
                  }

                yield! nextStep nextState (node :: path)
          | Result.Error e ->
            yield Result.Error e
      }
      |> AsyncSeq.cache

    yield! results

    // Record bad path when no solution is found
    let! solution =
      results
      |> AsyncSeq.choose ifOk
      |> AsyncSeq.tryFirst

    match solution with
    | Some _ -> ()
    | None ->
      let errors =
        results
        |> AsyncSeq.choose ifError
        |> AsyncSeq.distinctUntilChanged

      for ((p, cs), error) in errors do
        context.Console.Write (string error, LoggingLevel.Trace)
        do! resolver.PostAndAsyncReply (fun ch -> MarkBadPath (path, (p, cs), error, ch))
}

let solutionCollector resolutions = async {
  let! xs =
    resolutions
    |> AsyncSeq.take 2048
    |> AsyncSeq.takeWhileInclusive (Result.isOk >> not)
    |> AsyncSeq.toListAsync

  return
    xs
    |> List.tryLast
    |> Option.defaultValue (Result.Error (TransitiveConflict Set.empty))
}

let solve (context : TaskContext) (partialSolution : Solution) (manifest : Manifest) (style : ResolutionStyle) (lock : Lock option) = async {
  let hints =
    lock
    |> Option.map (fun l -> l.Packages |> (Map.map (fun _ v -> [ v ])))
    |> Option.defaultValue Map.empty

  let state =
    {
      Root =
        Set.union
          manifest.Dependencies
          manifest.PrivateDependencies
      Hints = hints
      Selections = partialSolution.Resolutions
      Locations = manifest.Locations
    }

  let sourceExplorer = OverrideSourceExplorer (context.SourceExplorer, manifest.Overrides)

  let resolver = resolutionManager sourceExplorer (createLogger context.Console (Some "solver"))

  let prefetcher = Prefetcher (sourceExplorer, 10)

  let strategy =
    match style with
    | Quick -> quickStrategy resolver sourceExplorer
    | Upgrading -> upgradeStrategy resolver sourceExplorer

  let resolutions =
    step context resolver prefetcher strategy state [ Root manifest ]

  let! result =
    resolutions
    |> AsyncSeq.map (Result.mapError snd)
    |> solutionCollector

  context.Console.Write (string result, LoggingLevel.Trace)

  return result
}

let fromLock (sourceExplorer : ISourceExplorer) (lock : Lock) : Async<Solution> = async {
  let rec packageLockToSolution (locked : LockedPackage) : Async<ResolvedVersion * Solution> = async {
    let! manifest =
      sourceExplorer.FetchManifest (locked.Location, locked.Versions)

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
      Versions = locked.Versions
      Lock = locked.Location
      Manifest = manifest
    }

    return (resolvedVersion, { Resolutions = resolutions |> Map.ofSeq })
  }

  let! resolutions =
    lock.Packages
    |> Map.toSeq
    |> AsyncSeq.ofSeq
    |> AsyncSeq.mapAsync (fun (package, lockedPackage) -> async {
      let! solution =
        lockedPackage
        |> packageLockToSolution

      return (package, solution)
    })
    |> AsyncSeq.toListAsync

  return
    {
      Resolutions =
        resolutions
        |> Map.ofSeq
    }
}

let unlock (solution : Solution) (packages : Set<PackageIdentifier>) : Solution =
  {
    Resolutions =
      solution.Resolutions
      |> Map.toSeq
      |> Seq.filter (fst >> packages.Contains >> not)
      |> Map.ofSeq
  }
