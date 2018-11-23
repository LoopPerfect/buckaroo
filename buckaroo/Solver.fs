namespace Buckaroo

module Solver = 

  open FSharp.Control

  // let versionPriority (v : Version) : int = 
  //   match v with 
  //   | Version.Latest -> 1
  //   | Version.Tag _ -> 2
  //   | Version.SemVerVersion _ -> 3
  //   | Version.Branch branch -> 
  //     match branch with 
  //     | "master" -> 4
  //     | _ -> 5
  //   | Version.Revision _ -> 6

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
  
  let unsatisfied (solution : Solution) (dependencies : Set<Dependency>) = 
    dependencies
    |> Seq.distinct
    |> Seq.filter (fun x -> 
      match solution |> Map.tryFind x.Package with 
      | Some y -> y.Version |> Constraint.satisfies x.Constraint |> not
      | None -> true
    )
    |> List.ofSeq

  // let solutionFromLock (sourceExplorer : ISourceExplorer) (lock : Lock) : Async<Solution> = async {
  //   let! resolvedVersions = 
  //     lock.Packages
  //     |> Map.toSeq
  //     |> Seq.map (fun (identifier, v) -> async {
  //       let version = fst v
  //       let location = snd v 
  //       let! manifest = sourceExplorer.FetchManifest location
  //       let resolvedVersion = {
  //         Version = version; 
  //         Location = location; 
  //         Manifest = manifest; 
  //       }
  //       return (identifier, resolvedVersion)
  //     })
  //     |> Async.Parallel
  //   return 
  //     resolvedVersions 
  //     |> Map.ofSeq
  // }

  type LocatedAtom = Atom * PackageLocation

  type Hints = LocatedAtom list

  let lockToHints (lock : Lock) : Hints = 
    lock.Packages 
    |> Map.toSeq
    |> Seq.map (fun (p, (v, location)) -> ({ Package = p; Version = v }, location))
    |> List.ofSeq

  type Depth = int


  type Constraints = Set<Dependency>

  type Visited = Set<PackageLocation>

  type SolverState = (Solution * Constraints * Visited * Depth * Hints)

  let rec step (sourceExplorer : ISourceExplorer) (state : SolverState) : AsyncSeq<Resolution> = asyncSeq {
    let (solution, dependencies, visited, depth, hints) = state

    let log (x : string) = 
      "[" + (string depth) + "] " + x
      |> System.Console.WriteLine

    // Sort the pending dependencies by cost
    let pending = 
      unsatisfied solution dependencies
      |> List.sortBy (fun x -> // TODO: Proper comparator! 
        match hints |> Seq.tryFind (fun (atom, _) -> atom |> Dependency.satisfies x) with
        | Some _ -> 
          // A hint that satisfies a difficult constraint is higher priority! 
          (constraintSearchCost x.Constraint) * -1
        | None -> constraintSearchCost x.Constraint
      )

    // Anything left to process? 
    match pending with
    | head :: _ -> 

      // Unpack available (and compatible) versions 
      let isVersionToExplore (version : Version) = 
        version |> Constraint.satisfies head.Constraint && 
        (
          dependencies 
          |> Seq.filter (fun y -> y.Package = head.Package)
          |> Seq.exists (fun y -> version |> Constraint.agreesWith y.Constraint |> not) 
          |> not
        )

      let locations : AsyncSeq<Version * PackageLocation> = 
        asyncSeq {
          yield!
            hints 
            |> Seq.filter (fun (atom, _) -> atom.Package = head.Package)
            |> Seq.map (fun (atom, location) -> (atom.Version, location))
            |> AsyncSeq.ofSeq
          
          for version in sourceExplorer.FetchVersions head.Package do
            if isVersionToExplore version
            then
              yield! 
                sourceExplorer.FetchLocations head.Package version
                |> AsyncSeq.map (fun location -> (version, location))
        }
        |> AsyncSeq.filter (fun (_, location) -> visited |> Set.contains location |> not)

      for (version, location) in locations do
        try 
          let! freshHintsTask = 
            async {
              try
                let! lock = sourceExplorer.FetchLock location
                return lockToHints lock
              with _ ->
                return []
            }
            |> Async.StartChild

          let! manifest = sourceExplorer.FetchManifest location

          let resolvedVersion =
            {
              Version = version; 
              Location = location; 
              Manifest = manifest; 
            }

          let! freshHints = freshHintsTask

          if freshHints |> Seq.isEmpty |> not
          then
            System.Console.WriteLine("Found hints: [" + (freshHints |> Seq.map fst |> Seq.map Atom.show |> String.concat ", ") + " ]")

          let nextState = 
            (
              solution 
              |> Map.add head.Package resolvedVersion, 
              dependencies
              |> Set.ofSeq
              |> Set.union resolvedVersion.Manifest.Dependencies, 
              visited |> Set.add location, 
              depth + 1, 
              hints |> List.append freshHints
            )
          yield! step sourceExplorer nextState
        with error -> 
          System.Console.WriteLine(error)
          yield Resolution.Error error
    | [] -> 
      yield Resolution.Ok solution
  }

  let solve (sourceExplorer : ISourceExplorer) (manifest : Manifest) (lock : Lock option) = async {
    let hints = 
      lock
      |> Option.map lockToHints
      |> Option.defaultValue []
    let resolutions = 
      step sourceExplorer (Map.empty, manifest.Dependencies |> Set.ofSeq, Set.empty, 0, hints)
    return
      resolutions
      // |> AsyncSeq.take 128
      |> AsyncSeq.filter (fun x -> 
        match x with
        | Ok _ -> true
        | _ -> false
      )
      |> AsyncSeq.take 1
      |> AsyncSeq.toListAsync
      |> Async.RunSynchronously
      |> List.tryHead
      |> Option.defaultValue (manifest.Dependencies |> Set.ofSeq |> Resolution.Conflict)
  }
