module Buckaroo.ResolveCommand

open System
open Buckaroo.RichOutput

let task (context : Tasks.TaskContext) partialSolution resolutionStyle = async {
  let log (x : RichOutput) = context.Console.Write x

  let logInfo (x : RichOutput) =
    ("info " |> text |> foreground ConsoleColor.Blue) + x
    |> log

  let logError (x : RichOutput) =
    ("error " |> text |> foreground ConsoleColor.Red) + x
    |> log

  let! maybeLock = async {
    try
      return!
        Tasks.readLockIfPresent
    with error ->
      logError ("The existing lock-file is invalid. " |> text)
      logInfo (
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

    logInfo <| (text "Resolve start: ") + (string resolveStart |> text |> foreground ConsoleColor.Cyan)

    "Resolving dependencies... " |> text |> logInfo

    let! resolution = Solver.solve context partialSolution manifest resolutionStyle maybeLock

    let resolveEnd = DateTime.Now

    logInfo <| (text "Resolve end: ") + (string resolveEnd |> text |> foreground ConsoleColor.Cyan)
    logInfo <| (text "Resolve time: ") + (resolveEnd - resolveStart |> string |> text |> foreground ConsoleColor.Cyan)

    match resolution with
    | Resolution.Failure f ->
      "Error! " |> text |> foreground ConsoleColor.Red |> log
      f.Constraint.ToString() + " for " + f.Package.ToString() + " coudn't be satisfied because: " + f.Msg
      |> string |> text |> log
    | Resolution.Conflict x ->
      "Conflict! " |> text |> foreground ConsoleColor.Red |> log
      x |> string |> text |> log

      return ()
    | Resolution.Error e ->
      "Error! " |> text |> foreground ConsoleColor.Red |> log
      e |> string |> text |> log
      return ()
    | Resolution.Ok solution ->
      "Success! " |> text |> foreground ConsoleColor.Green |> log
      let lock = Lock.fromManifestAndSolution manifest solution

      try
        let! previousLock = Tasks.readLock
        let diff = Lock.showDiff previousLock lock
        diff |> text |> log
      with _ ->
        ()

      do! Tasks.writeLock lock

      return ()
  }

  match (resolutionStyle, maybeLock) with
  | (Quick, Some lock) ->
    if lock.ManifestHash = Manifest.hash manifest
    then
      logInfo <| (text "The existing lock-file is already up-to-date! ")

      return ()
    else
    return! resolve
  | (_, _) ->
    return! resolve
}
