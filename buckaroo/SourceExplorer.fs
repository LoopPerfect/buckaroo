namespace Buckaroo

open FSharp.Control

type PackageSources = Map<AdhocPackageIdentifier, PackageSource>

type VersionedLock = PackageLock * Set<Version>

type ISourceExplorer =
  abstract member FetchVersions : PackageSources -> PackageIdentifier -> AsyncSeq<Version>
  abstract member FetchLocations : PackageSources -> PackageIdentifier -> Version -> AsyncSeq<PackageLocation>
  abstract member LockLocation : PackageLocation -> Async<PackageLock>
  abstract member FetchManifest : PackageLock -> Async<Manifest>
  abstract member FetchLock : PackageLock -> Async<Lock>

module SourceExplorer =
  let fetchLocationsForConstraint (sourceExplorer : ISourceExplorer) locations package versionConstraint =
    let rec loop (versionConstraint : Constraint) = asyncSeq {
      match Constraint.simplify versionConstraint with
      | Complement c ->
        let! complement =
          loop c
          |> AsyncSeq.map fst
          |> AsyncSeq.fold (fun s x -> Set.add x s) Set.empty

        yield!
          loop (Constraint.All [])
          |> AsyncSeq.filter (fun (location, _) -> complement |> Set.contains location |> not)
      | Any xs ->
        yield!
          xs
          |> List.distinct
          |> List.sortDescending
          |> List.map loop
          |> AsyncSeq.mergeAll
          |> AsyncSeq.distinctUntilChanged
      | All xs ->
        let combine (xs : Set<PackageLocation * Set<Version>>) (ys : Set<PackageLocation * Set<Version>>) =
          xs
          |> Seq.choose (fun (location, versions) ->
            let matchingVersions =
              ys
              |> Seq.filter (fst >> (=) location)
              |> Seq.collect snd
              |> Seq.toList

            match matchingVersions with
            | [] -> None
            | vs -> Some (location, versions |> Set.union (set vs))
          )
          |> Set.ofSeq

        let! complement =
          xs
          |> Seq.choose (fun x ->
            match x with
            | Complement c -> Some c
            | _ -> None
          )
          |> Seq.toList
          |> Constraint.Any
          |> loop
          |> AsyncSeq.map fst
          |> AsyncSeq.fold (fun s x -> Set.add x s) Set.empty

        let constraints =
          xs
          |> List.filter (fun x ->
            match x with
            | Complement _ -> false
            | _ -> true
          )
          |> List.sort
          |> List.distinct

        yield!
          (
            if Seq.isEmpty constraints
            then
              sourceExplorer.FetchVersions locations package
              |> AsyncSeq.collect (fun version ->
                sourceExplorer.FetchLocations locations package version
                |> AsyncSeq.map (fun location -> (location, Set.singleton version))
              )
            else
              constraints
              |> List.map (loop >> (AsyncSeq.scan (fun s x -> Set.add x s) Set.empty))
              |> List.reduce (AsyncSeq.combineLatestWith combine)
              |> AsyncSeq.concatSeq
          )
          |> AsyncSeq.filter (fun (location, _) -> Set.contains location complement |> not)
          |> AsyncSeq.distinctUntilChanged
      | Exactly version ->
        yield!
          sourceExplorer.FetchLocations locations package version
          |> AsyncSeq.map (fun location -> (location, Set.singleton version))
    }

    loop versionConstraint
