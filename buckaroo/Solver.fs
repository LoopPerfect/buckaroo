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

  let selectNextOpen (pending : Set<Dependency>) = 
    let cost (d : Dependency) = 
      match d.Constraint with 
      | Constraint.Exactly v -> 
        match v with 
        | Revision _ -> 0
        | Tag _ -> 1
        | SemVerVersion _ -> 2
        | Branch _ -> 3
        | Latest -> 4
      | Constraint.All _ -> 10
      | Constraint.Any _ -> 20
      | Constraint.Complement _ -> 30
    let pendingSorted = 
      pending
      |> Seq.toList
      |> List.sortBy cost
    match pendingSorted with 
    | head :: tail -> 
      Some (head, tail |> Seq.filter (fun x -> x <> head) |> Set.ofSeq)
    | [] -> None

  let fetchAvailableVersions (sourceManager : ISourceManager) (next : Dependency) : Async<ResolvedVersion list> = async {
    let! versions = sourceManager.FetchVersions next.Package
    let satisfactoryVersions = 
      versions
      |> Seq.filter (Constraint.satisfies next.Constraint)
      |> Seq.toList
    let! resolvedVersions = 
      satisfactoryVersions 
      |> Seq.map (fun version -> async {
        let! locations = sourceManager.FetchLocations next.Package version
        return!
          locations
          |> Seq.map (fun location -> async {
            try
              let! manifest = sourceManager.FetchManifest location
              return Some { 
                Version = version; 
                Location = location; 
                Manifest = manifest; 
              }
            with _ -> 
              // System.Console.WriteLine(error)
              return None
          })
          |> Async.Parallel
      })
      |> Async.Parallel
    return 
      resolvedVersions 
      |> Seq.collect id 
      |> Seq.choose id 
      |> Seq.toList
  }

  let rec step (sourceManager : ISourceManager) 
               (selected : Map<PackageIdentifier, ResolvedVersion>, 
                pending : Set<Dependency>, 
                closed : Set<Dependency>) = 
    async {
      match selectNextOpen pending with
      | Some (next, stillPending) -> 
        System.Console.WriteLine("Processing " + (Dependency.show next) + "... ")

        let! availableVersions = fetchAvailableVersions sourceManager next

        let compatibleVersions = 
          availableVersions
          |> Seq.filter (fun v -> 
            match selected |> Map.tryFind next.Package with 
            | Some p -> ResolvedVersion.isCompatible p v && Version.harmonious p.Version v.Version
            | None -> true
          )
          |> Seq.distinct
          |> Seq.toList
        
        let rank (v : Version) = 
          match v with
          | Version.Latest -> 4
          | Version.Revision _ -> 3
          | Version.Tag _ -> 2
          | Version.SemVerVersion _ -> 1
          | Version.Branch _ -> 0

        let sortedVersions = 
          compatibleVersions
          |> List.sortBy (fun x -> rank x.Version)

        let! resolutions = 
          sortedVersions
          |> Seq.distinct
          |> Seq.map (fun v -> async {
            try
              let! manifest = sourceManager.FetchManifest v.Location
              let nextSelected = 
                selected
                |> Map.add next.Package { Location = v.Location; Version = v.Version; Manifest = manifest }
              let nextPending = 
                manifest.Dependencies 
                |> Set.union stillPending
              let nextClosed = Set.add next closed
              return! step sourceManager (nextSelected, nextPending, nextClosed)
            with error -> 
              return Resolution.Error error
          })
          |> Async.Parallel

        let okResolution = 
          resolutions
          |> Seq.tryFind (fun x -> 
            match x with 
            | Resolution.Ok _ -> true 
            | _ -> false)

        let defaultResolution = 
          stillPending
          |> Set.add next
          |> Resolution.Conflict

        let failedResolution = 
          resolutions
          |> Seq.tryFind (fun x -> 
            match x with 
            | Resolution.Ok _ -> false 
            | _ -> true)
          |> Option.defaultValue defaultResolution

        return
          match okResolution with 
          | Some x -> x
          | None -> failedResolution

      | None -> return Resolution.Ok selected
    }

  let solve (sourceManager : ISourceManager) (manifest : Manifest) = 
    step sourceManager (Map.empty, manifest.Dependencies |> Set.ofSeq, Set.empty)
