module Buckaroo.AddCommand

open System.IO
open Buckaroo.RichOutput
open Buckaroo.Logger

let task (context : Tasks.TaskContext) dependencies = async {
  let logger = createLogger context.Console None

  logger.RichInfo (
    (text "Adding dependency on ") +
    (
      dependencies
      |> Seq.map Dependency.showRich
      |> RichOutput.concat (text " ")
    )
  )

  let! manifest = Tasks.readManifest "."

  let newManifest = {
    manifest with
      Dependencies =
        manifest.Dependencies
        |> Seq.append dependencies
        |> Set.ofSeq;
  }

  if manifest = newManifest
  then
    logger.Warning ("The dependency already exists in the manifest")
    return ()
  else
    let! maybeLock = async {
      if File.Exists(Constants.LockFileName)
      then
        let! lock = Tasks.readLock
        return Some lock
      else
        return None
    }

    let! resolution =
      Solver.solve context Solution.empty newManifest ResolutionStyle.Quick maybeLock

    match resolution with
    | Result.Ok solution ->
      do! Tasks.writeManifest newManifest
      do! Tasks.writeLock (Lock.fromManifestAndSolution newManifest solution)
      do! InstallCommand.task context

      logger.Success ("The dependency was added to the manifest and installed")
    | _ ->
      logger.Error ("Failed to add the dependency")
}
