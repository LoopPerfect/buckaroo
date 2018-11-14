namespace Buckaroo

type Solution = Map<PackageIdentifier, ResolvedVersion>

type Resolution = 
| Conflict of Set<Dependency>
| Error of System.Exception
| Ok of Solution

module Solver = 

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

  type SolverState = (Solution * Set<Dependency> * Set<ResolvedVersion>)

  let rec step (sourceManager : ISourceManager) (state : SolverState) = async {
    let (solution, dependencies, visited) = state
    let pending = unsatisfied solution dependencies
    // TODO: Prioritise pending list
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
      let! resolvedVersions = 
        compatibleVersions
        |> Seq.map (fun version -> async {
          let! locations = sourceManager.FetchLocations head.Package version
          let! resolvedVersions = 
            locations
            |> Seq.map (fun location -> async {
              try 
                let! manifest = sourceManager.FetchManifest location
                return {
                  Version = version; 
                  Location = location; 
                  Manifest = manifest;
                } |> Some
              with _ -> 
                return None
            })
            |> Async.Parallel
          return 
            resolvedVersions 
            |> Seq.choose id 
            |> Seq.filter (fun x -> visited |> Set.contains x |> not)
        })
        |> Async.Parallel
        |> (fun x -> async {
          let! y = x
          return y |> Seq.collect id |> Seq.toList
        })
      let solutions = 
        resolvedVersions
        |> Seq.map (fun x -> 
          let nextSolution = 
            solution 
            |> Map.add head.Package x
          let nextDependencies = 
            dependencies 
            |> Set.union x.Manifest.Dependencies
          let nextVisited = visited |> Set.add x
          step sourceManager (nextSolution, nextDependencies, nextVisited)
        )
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
