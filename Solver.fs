module Solver

open System

open Project
open ResolvedVersion
open Dependency
open Version
open Manifest

type Revision = string

type ResolvedPackage = { Project : Project; Revision : Revision; Version : Version }

type Resolution = 
| Conflict of Set<Dependency>
| Error of System.Exception
| Ok of Set<ResolvedPackage>

let show resolution = 
  match resolution with
  | Conflict xs -> "Conflict! " + (xs |> Seq.map Dependency.show |> String.concat " ")
  | Error e -> "Error! " + e.Message
  | Ok xs -> 
    "Success! " + (
      xs 
      |> Seq.map (fun x -> Project.show x.Project + "@" + Version.show x.Version + "(" + x.Revision + ")") 
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

let rec step (selected : Set<ResolvedPackage>, 
              pending : Set<Dependency>, 
              closed : Set<Dependency>) = 
  async {
    match selectNextOpen pending with
    | Some (next, stillPending) -> 
      let! availableVersions = async {
        let! versions = SourceManager.fetchVersions next.Project
        let satisfactoryVersions = 
          versions
          |> Seq.filter (Constraint.satisfies next.Constraint)
          |> Seq.toList
        let! resolvedVersions = 
          satisfactoryVersions 
          |> Seq.map (fun v -> async {
            let! revisions = SourceManager.fetchRevisions next.Project v
            return 
              revisions
              |> Seq.map (fun r -> { Version = v; Revision = r })
              |> Seq.toList
          })
          |> Async.Parallel
        return resolvedVersions |> Seq.collect (fun x -> x) |> Seq.toList
      }

      let compatibleVersions = 
        availableVersions
        |> Seq.filter (fun v -> 
          selected 
          |> Seq.filter (fun p -> p.Project = next.Project)
          |> Seq.forall (fun p -> p.Revision = v.Revision && Version.harmonious p.Version v.Version)
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
        |> Seq.map (fun v -> async {
          try
            let! manifest = SourceManager.fetchManifest next.Project v.Revision
            let nextSelected = 
              selected
              |> Set.add { Project = next.Project; Revision = v.Revision; Version = v.Version }
            let nextPending = 
              manifest.Dependencies 
              |> Set.union stillPending
            let nextClosed = Set.add next closed
            return! step (nextSelected, nextPending, nextClosed)
          with error -> 
            return Resolution.Error error
        })
        |> Seq.map Async.RunSynchronously
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

let solve (manifest : Manifest) = 
  step (Set.empty, manifest.Dependencies |> Set.ofSeq, Set.empty)
