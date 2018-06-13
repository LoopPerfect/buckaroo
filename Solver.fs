module Solver

open ResolvedVersion
open Dependency
open Version
open Manifest
open ResolvedPackage

type Revision = string
type SourceManager = SourceManager.SourceManager

type Solution = Map<Project, ResolvedVersion>

type Resolution = 
| Conflict of Set<Dependency>
| Error of System.Exception
| Ok of Solution

let show resolution = 
  match resolution with
  | Conflict xs -> "Conflict! " + (xs |> Seq.map Dependency.show |> String.concat " ")
  | Error e -> "Error! " + e.Message
  | Ok xs -> 
    "Success! " + (
      xs 
      |> Seq.map (fun x -> Project.show x.Key + "@" + ResolvedVersion.show x.Value) 
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
    | Constraint.All _ -> 10
    | Constraint.Any _ -> 20
    | Constraint.Complement _ -> 30
  let pendingSorted = 
    pending
    |> Seq.toList
    |> List.sortBy cost
  match pendingSorted with 
  | head :: tail -> Some (head, Set.ofSeq tail)
  | [] -> None

let rec step (sourceManager : SourceManager) 
             (selected : Map<Project, ResolvedVersion>, 
              pending : Set<Dependency>, 
              closed : Set<Dependency>) = 
  async {
    match selectNextOpen pending with
    | Some (next, stillPending) -> 
      let! availableVersions = async {
        let! versions = sourceManager.FetchVersions next.Project
        let satisfactoryVersions = 
          versions
          |> Seq.filter (Constraint.satisfies next.Constraint)
          |> Seq.toList
        let! resolvedVersions = 
          satisfactoryVersions 
          |> Seq.map (fun v -> async {
            let! revisions = sourceManager.FetchRevisions next.Project v
            return 
              revisions
              |> Seq.map (fun r -> { Version = v; Revision = r })
              |> Seq.toList
          })
          |> Async.Parallel
        return resolvedVersions |> Seq.collect id |> Seq.toList
      }

      let compatibleVersions = 
        availableVersions
        |> Seq.filter (fun v -> 
          match Map.tryFind next.Project selected with 
          | Some p -> p.Revision = v.Revision && Version.harmonious p.Version v.Version
          | None -> true
        )
        |> Seq.toList
      
      let rank (v : Version) = 
        match v with
        | Version.Revision _ -> 3
        | Version.Tag _ -> 2
        | Version.SemVerVersion _ -> 1
        | Version.Branch _ -> 0

      let sortedVersions = 
        compatibleVersions
        |> List.sortBy (fun x -> rank x.Version)

      let resolutions = 
        sortedVersions
        |> Seq.map (fun v -> Async.RunSynchronously (async {
          try
            let! manifest = sourceManager.FetchManifest next.Project v.Revision
            let nextSelected = 
              selected
              |> Map.add next.Project { Revision = v.Revision; Version = v.Version }
            let nextPending = 
              manifest.Dependencies 
              |> Set.union stillPending
            let nextClosed = Set.add next closed
            return! step sourceManager (nextSelected, nextPending, nextClosed)
          with error -> 
            return Resolution.Error error
        }))
        |> Seq.toList

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

let solve (sourceManager : SourceManager) (manifest : Manifest) = 
  step sourceManager (Map.empty, manifest.Dependencies |> Set.ofSeq, Set.empty)
