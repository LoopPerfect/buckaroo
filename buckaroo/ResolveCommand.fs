module Buckaroo.ResolveCommand

open System
open Buckaroo.RichOutput
open Buckaroo.Logger
open Buckaroo.SearchStrategy

let task (context : Tasks.TaskContext) partialSolution resolutionStyle = async {
  let logger = createLogger context.Console None

  let! maybeLock = async {
    try
      return!
        Tasks.readLockIfPresent
    with error ->
      logger.Error "The existing lock-file is invalid. "
      logger.RichInfo (
        (text "Perhaps you want to delete ") +
        (text "buckaroo.lock.toml" |> foreground ConsoleColor.Magenta) +
        (text " and try again?")
      )

      return!
        raise error
  }

  let! manifest = Tasks.readManifest "."

  let resolve = async {
    let resolveStart = DateTime.Now

    logger.RichInfo <| (text "Resolve start: ") + (resolveStart |> Toml.formatDateTime |> text |> foreground ConsoleColor.Cyan)

    let styleName =
      match resolutionStyle with
      | Quick -> "quick"
      | Upgrading -> "upgrade"
      |> text
      |> foreground ConsoleColor.Cyan

    (text "Resolving dependencies using ") + (styleName) + " strategy... " |> logger.RichInfo

    let! resolution =
      Solver.solve context partialSolution manifest resolutionStyle maybeLock

    let resolveEnd = DateTime.Now

    logger.RichInfo <| (text "Resolve end: ") + (resolveEnd |> Toml.formatDateTime |> text |> foreground ConsoleColor.Cyan)
    logger.RichInfo <| (text "Resolve time: ") + (resolveEnd - resolveStart |> string |> text |> foreground ConsoleColor.Cyan)

    match resolution with
    | Result.Error e ->
      (SearchStrategyError.show e) |> logger.RichError

      return false
    | Result.Ok solution ->
      "A solution to the constraints was found. " |> logger.Success
      let lock = Lock.fromManifestAndSolution manifest solution

      try
        let! previousLock = Tasks.readLock
        let diff = Lock.showDiff previousLock lock
        diff |> text |> logger.RichInfo
      with _ ->
        ()

      do! Tasks.writeLock lock

      "The lock-file was updated. " |> logger.Success

      return true
  }

  match (resolutionStyle, maybeLock) with
  | (Quick, Some lock) ->
    if lock.ManifestHash = Manifest.hash manifest
    then
      logger.RichInfo <| (text "The existing lock-file is already up-to-date! ")

      return true
    else
    return! resolve
  | (_, _) ->
    return! resolve
}
