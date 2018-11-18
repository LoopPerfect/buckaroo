namespace Buckaroo

type Solution = Map<PackageIdentifier, ResolvedVersion>

type Resolution = 
| Conflict of Set<Dependency>
| Error of System.Exception
| Ok of Solution

module Solver = 

  let versionSearchCost (v : Version) : int = 
    match v with 
    | Version.Revision _ -> 0
    | Version.Latest -> 1
    | Version.Tag _ -> 2
    | Version.SemVerVersion _ -> 3
    | Version.Branch branch -> 
      match branch with 
      | "master" -> 4
      | _ -> 5

  let rec constraintSearchCost (c : Constraint) : int = 
    match c with 
    | Constraint.Exactly v -> versionSearchCost v
    | Constraint.Any xs -> 
      xs |> Seq.map constraintSearchCost |> Seq.append [ 9 ] |> Seq.min
    | Constraint.All xs -> 
      xs |> Seq.map constraintSearchCost |> Seq.append [ 0 ] |> Seq.max
    | Constraint.Complement _ -> 6

  let show resolution = 
    match resolution with
    | Conflict xs -> "Conflict! " + (xs |> Seq.map Dependency.show |> String.concat " ")
    | Error e -> "Error! " + e.Message
    | Ok xs -> 
      "Success! " + (
        xs 
        |> Seq.map (fun x -> PackageIdentifier.show x.Key + "@" + ResolvedVersion.show x.Value) 
        |> String.concat " "
      )

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

  let rec step (sourceManager : ISourceManager) (state : SolverState) = async {
    let (solution, dependencies, visited) = state
    let pending = 
      unsatisfied solution dependencies
      |> List.sortBy (fun x -> constraintSearchCost x.Constraint)
    match pending with
    | head :: _ -> 
      System.Console.WriteLine("Processing " + (Dependency.show head) + "... ")
      let! versions = sourceManager.FetchVersions head.Package
      let compatibleVersions = 
        versions
        |> Seq.filter (fun x -> x |> Constraint.satisfies head.Constraint)
        |> Seq.filter (fun x -> 
          dependencies 
          |> Seq.exists (fun y -> x |> Constraint.agreesWith y.Constraint |> not) 
          |> not
        )
        |> Seq.distinct
      let resolvedVersions = 
        compatibleVersions
        |> Seq.sortBy (fun x -> x |> versionSearchCost)
        |> Seq.map (fun version -> async {
          let! locations = sourceManager.FetchLocations head.Package version
          let newLocations =
            locations
            |> Seq.filter (fun x -> visited |> Set.contains x |> not)
          return (version, newLocations)
        })
        |> Async.Parallel
        |> Async.RunSynchronously
        |> Seq.collect (fun (version, locations) -> 
            locations
            |> Seq.map (fun location -> async {
              try 
                let! manifest = sourceManager.FetchManifest location
                return {
                  Version = version; 
                  Location = location; 
                  Manifest = manifest;
                } |> Some
              with error -> 
                System.Console.WriteLine(error)
                return None
            })
        )

      let solutions = 
        resolvedVersions
        |> Seq.choose Async.RunSynchronously
        |> Seq.map (fun x -> 
          let nextSolution = 
            solution 
            |> Map.add head.Package x
          let nextDependencies = 
            dependencies 
            |> Set.union x.Manifest.Dependencies
          let nextVisited = visited |> Set.add x.Location
          step sourceManager (nextSolution, nextDependencies, nextVisited)
        )
        // |> Seq.chunkBySize 8
        // |> Seq.map Async.Parallel
        // |> Seq.map Async.RunSynchronously
        // |> Seq.collect id
        |> Seq.map Async.RunSynchronously

      return
        solutions 
        // TODO: Take many solutions and pick the best
        |> Seq.tryFind (fun x -> 
          match x with 
          | Resolution.Ok _ -> true
          | _ -> false
        )
        |> Option.defaultValue (pending |> Set.ofSeq |> Resolution.Conflict)
    | [] -> 
      return Resolution.Ok solution
  }

  let solve (sourceManager : ISourceManager) (manifest : Manifest) = 
    step sourceManager (Map.empty, manifest.Dependencies |> Set.ofSeq, Set.empty)
