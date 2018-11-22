namespace Buckaroo

module Solver = 

  open FSharp.Control

  let versionPriority (v : Version) : int = 
    match v with 
    | Version.Latest -> 1
    | Version.Tag _ -> 2
    | Version.SemVerVersion _ -> 3
    | Version.Branch branch -> 
      match branch with 
      | "master" -> 4
      | _ -> 5
    | Version.Revision _ -> 6

  let versionSearchCost (v : Version) : int = 
    match v with 
    | Version.Revision _ -> 0
    | Version.Latest -> 1
    | Version.Tag _ -> 2
    | Version.SemVerVersion _ -> 3
    | Version.Branch _ -> 4

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

  type SolverState = (Solution * Set<Dependency> * Set<PackageLocation>)

  let rec step (sourceManager : ISourceManager) (state : SolverState) : AsyncSeq<Resolution> = asyncSeq {
    let (solution, dependencies, visited) = state

    // Sort the pending dependencies by cost
    let pending = 
      unsatisfied solution dependencies
      |> List.sortBy (fun x -> constraintSearchCost x.Constraint)

    // Anything left to process? 
    match pending with
    | head :: tail -> 
      // TODO: Better notification system
      System.Console.WriteLine("Processing " + (Dependency.show head) + "... ")

      match solution |> Map.tryFind head.Package with 
      | Some existingSelection -> 
        System.Console.WriteLine("Already have " + (Dependency.show head) + " as " + (ResolvedVersion.show existingSelection) + "! ")
        if existingSelection.Version |> Constraint.agreesWith head.Constraint
        then
          let nextState = 
            (
              solution, 
              tail
              |> Set.ofSeq, 
              visited
            )
          yield! step sourceManager nextState
        else
          yield Resolution.Conflict (set [ head ])
      | None -> 
        // Unpack available (and compatible) versions 
        let! versions = sourceManager.FetchVersions head.Package

        let compatibleVersions = 
          versions
          |> Seq.filter (fun x -> x |> Constraint.satisfies head.Constraint)
          |> Seq.filter (fun x -> 
            dependencies 
            |> Seq.filter (fun y -> y.Package = head.Package)
            |> Seq.exists (fun y -> x |> Constraint.agreesWith y.Constraint |> not) 
            |> not
          )
          |> Seq.distinct
          |> Seq.sortBy (fun x -> x |> versionPriority)

        for version in compatibleVersions do
          let! locations = sourceManager.FetchLocations head.Package version
          let freshLocations = 
            locations
            |> Seq.filter (fun x -> visited |> Set.contains x |> not)
          
          for location in freshLocations do 
            try 
              let! manifest = sourceManager.FetchManifest location
              let resolvedVersion =
                {
                  Version = version; 
                  Location = location; 
                  Manifest = manifest; 
                }
              let nextState = 
                (
                  solution 
                  |> Map.add head.Package resolvedVersion, 
                  tail
                  |> Set.ofSeq
                  |> Set.union resolvedVersion.Manifest.Dependencies, 
                  visited |> Set.add location
                )
              yield! step sourceManager nextState
            with error -> 
              System.Console.WriteLine(error)
              yield Resolution.Error error
    | [] -> 
      yield Resolution.Ok solution
  }

  let solve (sourceManager : ISourceManager) (manifest : Manifest) = async {
    let resolutions = 
      step sourceManager (Map.empty, manifest.Dependencies |> Set.ofSeq, Set.empty)
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
