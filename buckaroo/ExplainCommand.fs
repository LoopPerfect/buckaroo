module Buckaroo.ExplainCommand

open FSharp.Control
open Buckaroo.Tasks
open Buckaroo.Logger

let private explain (logger : Logger) (sourceExplorer : ISourceExplorer) (packageToExplain : PackageIdentifier) (lock : Lock) = async {
  let rec computeTraces traces = asyncSeq {
    for trace in traces do
      match trace with
      | head :: tail ->
        let package, isPrivate = head

        match lock.Packages |> Map.tryFind package with
        | Some lockedPackage ->
          yield head :: tail

          logger.Info ("Exploring " + (PackageIdentifier.show package) + "... ")

          let! manifest =
            sourceExplorer.FetchManifest (lockedPackage.Location, lockedPackage.Versions)

          let nextTraces =
            manifest.Dependencies
            |> Seq.map (fun dependency -> (dependency, false))
            |> Seq.append (manifest.PrivateDependencies |> Seq.map (fun dependency -> (dependency, true)))
            |> Seq.map (fun (dependency, isPrivate) -> (dependency.Package, isPrivate) :: head :: tail)
            |> Set.ofSeq

          yield! nextTraces |> AsyncSeq.ofSeq

          yield! computeTraces nextTraces
        | None -> ()
      | [] -> ()
  }

  let directDependencies =
    lock.Dependencies
    |> Seq.choose (fun target ->
      match target.PackagePath with
      | [], package -> Some [ (package, false) ]
      | _ -> None
    )
    |> Set.ofSeq

  let! traces =
    computeTraces directDependencies
    |> AsyncSeq.filter (fun trace ->
      match trace with
      | (head, _) :: _ -> head = packageToExplain
      | _ -> false
    )
    |> AsyncSeq.distinctUntilChanged
    |> AsyncSeq.toListAsync

  return
    traces
    |> Seq.sortBy List.length
    |> Seq.distinct
    |> Seq.toList
}

let task (context : TaskContext) (package : PackageIdentifier) = async {
  let logger = createLogger context.Console None

  logger.Info "Reading lock-file... "

  match! Tasks.readLockIfPresent with
  | Some lock ->
    logger.Info "Fetching traces... "

    let! traces = explain logger context.SourceExplorer package lock

    if Seq.isEmpty traces
    then
      logger.Success ("There are no traces for " + (PackageIdentifier.show package) + ". ")
    else
      logger.Success ("Found the following traces for " + (PackageIdentifier.show package) + ": ")

      for trace in traces do
        logger.Print
          <| " @ " +
            (
              trace
              |> List.rev
              |> Seq.map (fun (package, isPrivate) ->
                let arrow =
                  if isPrivate
                  then
                    "--{private}--> "
                  else
                    "-----> "
                arrow + PackageIdentifier.show package
              )
              |> String.concat " "
            )

    return 0
  | None ->
    logger.Warning "No lock-file is present. Run buckaroo resolve first. "
    return 1
}
