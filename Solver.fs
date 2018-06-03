module Solver

open Project
open ResolvedVersion
open Dependency
open Version
open Manifest

type ResolvedPackage = { Project : Project; Resolution : ResolvedVersion }

type Resolution = 
| Conflict of Set<Dependency>
| Ok of Set<ResolvedPackage>

let show resolution = 
  match resolution with
  | Conflict xs -> "Conflict! " + (xs |> Seq.map Dependency.show |> String.concat " ")
  | Ok xs -> 
    "Success! " + (
      xs 
      |> Seq.map (fun x -> Project.show x.Project + "@" + ResolvedVersion.show x.Resolution) 
      |> String.concat " "
    )

let selectNextOpen (pending : Set<Dependency>) = 
  // TODO: Prioritise correctly 
  match Seq.toList pending with 
  | head :: tail -> Some (head, tail |> Set.ofSeq )
  | [] -> None

let rec step (selected : Set<ResolvedPackage>, 
              pending : Set<Dependency>, 
              closed : Set<Dependency>) = 
  async {
    match selectNextOpen pending with
    | Some (next, stillPending) -> 

      let! availableVersions = async {
        match selected |> Seq.tryFind (fun x -> x.Project = next.Project) with 
        | Some resolvedPackage -> 
          match Constraint.satisfies next.Constraint resolvedPackage.Resolution.Version with
          | true -> return [ resolvedPackage.Resolution ]
          | _ -> return []
        | None -> 
          let! versions = SourceManager.fetchVersions next.Project
          let! resolvedVersions = 
            versions
            |> Seq.filter (Constraint.satisfies next.Constraint)
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

      // TODO: Improve sort
      let rank (v : Version) = 
        match v with
        | Version.Revision _ -> 3
        | Version.Tag _ -> 2
        | Version.SemVerVersion _ -> 1
        | Version.Branch _ -> 0

      let sortedVersions = 
        availableVersions
        |> List.sortBy (fun x -> rank x.Version)

      let resolutions = 
        sortedVersions
        |> Seq.map (fun v -> async {
          let! manifest = SourceManager.fetchManifest next.Project v.Revision
          let nextSelected = 
            selected
            |> Set.add { Project = next.Project; Resolution = v }
          let nextPending = 
            manifest.Dependencies 
            |> Set.ofList 
            |> Set.union stillPending
          let nextClosed = Set.add next closed
          return! step (nextSelected, nextPending, nextClosed)
        })
        |> Seq.map Async.RunSynchronously
        |> Seq.filter (fun x -> 
          match x with 
          | Resolution.Ok _ -> true 
          | _ -> false)
        |> Seq.take 1
        |> Seq.toList

      let resolution = 
        match resolutions with 
        | head :: _ -> head
        | _ -> Resolution.Conflict stillPending

      return resolution

    | None -> return Resolution.Ok selected
  }

let solve (manifest : Manifest) = 
  step (Set.empty, manifest.Dependencies |> Set.ofSeq, Set.empty)
