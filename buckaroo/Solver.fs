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

  type SolverState = (Solution * Set<Dependency> * Set<PackageLocation> * int)

  let rec step (sourceExplorer : ISourceExplorer) (state : SolverState) : AsyncSeq<Resolution> = asyncSeq {
    let (solution, dependencies, visited, depth) = state

    let log (x : string) = 
      "[" + (string depth) + "] " + x
      |> System.Console.WriteLine

    // Sort the pending dependencies by cost
    let pending = 
      unsatisfied solution dependencies
      |> List.sortBy (fun x -> constraintSearchCost x.Constraint)

    // Anything left to process? 
    match pending with
    | head :: tail -> 
      // TODO: Better notification system
      // log("Processing " + (Dependency.show head) + "... ")

      // Unpack available (and compatible) versions 
      let versions = 
        sourceExplorer.FetchVersions head.Package
        |> AsyncSeq.filter (fun x -> x |> Constraint.satisfies head.Constraint)
        |> AsyncSeq.filter (fun x -> 
          dependencies 
          |> Seq.filter (fun y -> y.Package = head.Package)
          |> Seq.exists (fun y -> x |> Constraint.agreesWith y.Constraint |> not) 
          |> not
        )

      for version in versions do
        log("Exploring " + (PackageIdentifier.show head.Package) + "@" + (Version.show version) + "... ")

        let freshLocations = 
          sourceExplorer.FetchLocations head.Package version
          |> AsyncSeq.filter (fun x -> visited |> Set.contains x |> not)

        for location in freshLocations do
          try 
            let! manifest = sourceExplorer.FetchManifest location

            // We can grab the lock-file to pre-empt upcoming dependencies... 
            // do! 
            //   async {
            //     let! lock = sourceExplorer.FetchLock location
            //     return!
            //       lock.Packages 
            //       |> Map.toSeq 
            //       |> Seq.map snd
            //       |> Seq.map sourceExplorer.Prepare
            //       |> Seq.map Async.Catch
            //       |> Async.Parallel
            //   }
            //   |> Async.Catch
            //   |> Async.Ignore
            //   |> Async.StartChild
            //   |> Async.Ignore

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
                dependencies
                |> Set.ofSeq
                |> Set.union resolvedVersion.Manifest.Dependencies, 
                visited |> Set.add location, 
                depth + 1
              )
            yield! step sourceExplorer nextState
          with error -> 
            System.Console.WriteLine(error)
            yield Resolution.Error error
    | [] -> 
      yield Resolution.Ok solution
  }

  let solve (sourceExplorer : ISourceExplorer) (manifest : Manifest) = async {
    let resolutions = 
      step sourceExplorer (Map.empty, manifest.Dependencies |> Set.ofSeq, Set.empty, 0)
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
