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
    Root: Manifest
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


  let trimSelections (selections: Map<PackageIdentifier, ResolvedVersion>) (deps: Set<Dependency>) =

    let rec loop (visited: Set<PackageIdentifier>) (deps: Set<Dependency>) : seq<PackageIdentifier * ResolvedVersion> = seq {
      let notVisited =
        deps
        |> Seq.filter (fun d -> visited |> Set.contains d.Package |> not)
        |> Seq.toList

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
    let c = constraints.[dep.Package] |> Seq.toList  |> All |> Constraint.simplify
    selections
    |> Map.tryFind dep.Package
    |> Option.map (fun rv -> rv.Versions |> Constraint.satisfies c |> not)
    |> Option.defaultValue true

  let findUnresolved pick (selections: Map<PackageIdentifier, ResolvedVersion>) (deps: Set<Dependency>) =

    let constraints =
      Map.valueList selections
      |> List.map (fun m -> m.Manifest.Dependencies)
      |> List.fold Set.union Set.empty
      |> constraintsOf

    let rec loop (visited: Set<PackageIdentifier>) (deps: Set<Dependency>) : seq<PackageIdentifier * Set<Constraint>> = seq {
      let notVisited =
        deps
        |> Seq.filter (fun d -> visited |> Set.contains d.Package |> not)
        |> Seq.toList

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

  type SearchStrategyError =
  | LimitReached of PackageIdentifier * Set<Constraint>
  | Unsatisfiable of PackageIdentifier * Set<Constraint>
  | TransitiveFailure of List<SearchStrategyError>

  type SearchStrategy = ISourceExplorer -> SolverState -> AsyncSeq<Result<PackageIdentifier * LocatedVersionSet, SearchStrategyError>>

  let fetchCandidatesForConstraint sourceExplorer locations (dep : Dependency) = asyncSeq {
    let candidatesToExplore = SourceExplorer.fetchLocationsForConstraint sourceExplorer locations dep.Package dep.Constraint

    let mutable hasCandidates = false
    let mutable branchFailures = Map.empty

    for x in candidatesToExplore do
      if branchFailures |> Map.exists (fun _ v -> v > MaxConsecutiveFailures) then
        let d = (dep.Package, Set [dep.Constraint])
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
                yield Result.Ok (dep.Package, (packageLocation, c))

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
              let d = (dep.Package, Set xs)
              yield d |> Unsatisfiable |> Result.Error
            }
          | FetchResult.Unsatisfiable u -> asyncSeq {
              let d = (dep.Package, Set[u])
              yield d |> Unsatisfiable |> Result.Error
            }

    if hasCandidates = false
    then
      let d = (dep.Package, Set [dep.Constraint])
      yield
        LimitReached d
        |> Result.Error
  }


  type ResolutionRequest = Map<PackageIdentifier, ResolvedVersion> * Dependency * PackageSources * AsyncReplyChannel<AsyncSeq<ResolvedVersion>>

  let resolutionManger (sourceExplorer : ISourceExplorer) : MailboxProcessor<ResolutionRequest> = MailboxProcessor.Start(fun inbox -> async {
    let mutable badManifests : Map<ResolvedVersion, SearchStrategyError> = Map.empty
    let mutable badDeps  : Map<Dependency, SearchStrategyError> = Map.empty

    let fetch selections locations dep = asyncSeq {
      for candidate in fetchCandidatesForConstraint sourceExplorer locations dep do
        match candidate with
        | Result.Error (TransitiveFailure _) -> ()
        | Result.Error (Unsatisfiable d) ->
          badDeps <- (badDeps |> Map.add d (Unsatisfiable d))
          yield Result.Error <| Unsatisfiable d
        | Result.Error (LimitReached d) ->
          badDeps <- (badDeps |> Map.add d (LimitReached d))
          yield Result.Error <| LimitReached d
        | Result.Ok (_, (location, versions)) ->
          let! lock = sourceExplorer.LockLocation location
          let! manifest = sourceExplorer.FetchManifest (lock, versions)
          let resolvedVersion : ResolvedVersion = {
            Manifest = manifest
            Versions = versions
            Lock = lock
          }

          let constraints = constraintsOfSelection selections
          let badSet = badDeps |> Map.keySet
          let isBad =
            constraints
            |> Map.toSeq
            |> Seq.map (fun (p, cs) ->  p  )


          ()

        //allRevisions <- (allRevisions |> Set.add (dep.Package, resolvedVersion))

        ()
    }

    while true do
      let! (selections, dep, locations, channel) = inbox.Receive()
      let candidates = fetch locations dep

      ()
  })


  let rec private step (context : TaskContext) (strategy : SearchStrategy) (state : SolverState) : AsyncSeq<SolverState> = asyncSeq {

    let sourceExplorer = context.SourceExplorer
    let log = namespacedLogger context.Console ("solver")

    let manifests =
      state.Selections
      |> Map.valueList
      |> Seq.map (fun rv -> rv.Manifest)
      |> Seq.append [state.Root]
      |> Seq.toList

    let locations =
      manifests
      |> Seq.map (fun m -> m.Locations |> Map.toSeq)
      |> Seq.fold Seq.append (Seq.ofList[])
      |> Map.ofSeq

    let unresolved = depthFirst state.Selections state.Root.Dependencies

    for (p, cs) in unresolved do
      let c = cs |> Seq.toList |> All

      let hints =
        state.Hints
        |> Map.tryFind p
        |> Option.defaultValue([])
        |> Seq.filter(fun (atom, _) -> atom.Versions |> Constraint.satisfies c)
        |> AsyncSeq.ofSeq
        |> AsyncSeq.mapAsync(fun (atom, location) -> async {
          let! lock = sourceExplorer.LockLocation location
          let! manifest = sourceExplorer.FetchManifest (lock, atom.Versions)
          let resolvedVersion = {
            Lock = lock
            Versions = atom.Versions
            Manifest = manifest
          }
          resolvedVersion
        })

      let candidates = SourceExplorer.fetchLocationsForConstraint sourceExplorer locations p c

      for candidate in candidates do
        match candidate with
        | Candidate (location, versions) ->
          let! lock = sourceExplorer.LockLocation location
          let! manifest = sourceExplorer.FetchManifest (lock, versions)
          let resolvedVersion = {
            Lock = lock
            Versions = versions
            Manifest = manifest
          }

          let nextState = {state with Selections = state.Selections |> Map.add p resolvedVersion}

          yield! step context strategy nextState
    ()

    yield state
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
    let hints =
      lock
      |> Option.map (lockToHints >> AsyncSeq.ofSeq)
      |> Option.defaultValue AsyncSeq.empty




    let resolutions =
      step context strategy state manifest

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
