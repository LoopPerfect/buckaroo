namespace Buckaroo

open FSharp.Control

type PackageSources = Map<AdhocPackageIdentifier, PackageSource>

type VersionedLock = PackageLock * Set<Version>

type ISourceExplorer =
  abstract member FetchVersions : PackageSources -> PackageIdentifier -> AsyncSeq<Version>
  abstract member FetchLocations : PackageSources -> PackageIdentifier -> Version -> AsyncSeq<PackageLocation>
  abstract member LockLocation : PackageLocation -> Async<PackageLock>
  abstract member FetchManifest : PackageLock * Set<Version> -> Async<Manifest>
  abstract member FetchLock : PackageLock * Set<Version> -> Async<Lock>

type FetchResult =
| Candidate of PackageLocation * Set<Version>
| Unsatisfiable of Constraint

module SourceExplorer =

  let private appendIfEmpty y (xs : AsyncSeq<_>) = asyncSeq {
    let mutable hasYielded = false

    for x in xs do
      yield x
      hasYielded <- true

    if not hasYielded
    then
      yield y
  }

  let private candidates xs =
    xs
    |> Seq.choose (fun x ->
      match x with
      | Candidate (location, versions) -> Some (location, versions)
      | _ -> None
    )

  let fetchLocationsForConstraint (sourceExplorer : ISourceExplorer) locations package versionConstraint =
    let rec loop (versionConstraint : Constraint) : AsyncSeq<FetchResult> = asyncSeq {
      match Constraint.simplify versionConstraint with
      | Complement c ->
        let! complement =
          loop c
          |> AsyncSeq.choose (fun x ->
            match x with
            | Candidate (location, _) -> Some location
            | _ -> None
          )
          |> AsyncSeq.fold (fun s x -> Set.add x s) Set.empty

        yield!
          loop (Constraint.All Set.empty)
          |> AsyncSeq.filter (fun x ->
            match x with
            | Candidate (location, _) ->
              complement
              |> Set.contains location
              |> not
            | _ -> true
          )
      | Any xs ->
        yield!
          xs
          |> Set.toList
          |> List.sortDescending
          |> List.map loop
          |> AsyncSeq.mergeAll
          |> AsyncSeq.filter (fun x ->
            match x with
            | Candidate _ -> true
            | Unsatisfiable _ -> false
          )
          |> AsyncSeq.distinctUntilChanged
          |> appendIfEmpty (Unsatisfiable versionConstraint)
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

        yield!
          (
            if Seq.isEmpty xs
            then
              sourceExplorer.FetchVersions locations package
              |> AsyncSeq.collect (fun version ->
                sourceExplorer.FetchLocations locations package version
                |> AsyncSeq.map (fun location -> Candidate (location, Set.singleton version))
              )
            else
              xs
              |> Set.toList
              |> List.sort
              |> List.map (loop >> (AsyncSeq.scan (fun s x -> Set.add x s) Set.empty))
              |> List.reduce (AsyncSeq.combineLatestWith (fun x y ->
                let maybeUnsatisfiable =
                  x
                  |> Set.union y
                  |> Seq.tryFind (fun x ->
                    match x with
                    | Unsatisfiable _ -> true
                    | _ -> false
                  )

                match maybeUnsatisfiable with
                | Some unsat -> Set.singleton unsat
                | None ->
                  let a = set (candidates x)
                  let b = set (candidates y)

                  combine a b |> Set.map Candidate
              ))
              |> AsyncSeq.concatSeq
          )
          |> AsyncSeq.distinctUntilChanged
          |> appendIfEmpty (Unsatisfiable versionConstraint)

      | Exactly version ->
        yield!
          sourceExplorer.FetchLocations locations package version
          |> AsyncSeq.map (fun location -> Candidate (location, Set.singleton version))
          |> appendIfEmpty (Unsatisfiable versionConstraint)
      | Range (op, v) ->
        yield!
          sourceExplorer.FetchVersions locations package
          |> AsyncSeq.choose (fun version ->
            match version with
            | SemVer v -> Some v
            | _ -> None)
          |> AsyncSeq.filter (Constraint.isWithinRange (op, v))
          |> AsyncSeq.map (Version.SemVer)
          |> AsyncSeq.collect (fun version ->
            sourceExplorer.FetchLocations locations package version
            |> AsyncSeq.map (fun l -> Candidate (l, Set.singleton version))
          )
          |> appendIfEmpty (Unsatisfiable versionConstraint)
    }

    loop versionConstraint
    |> AsyncSeq.takeWhileInclusive (fun x ->
      match x with
      | Candidate _ -> true
      | Unsatisfiable _ -> false
    )