module Buckaroo.ResolveCommand

open System
open Buckaroo.RichOutput

let task (context : Tasks.TaskContext) resolutionStyle = async {
  let log (x : RichOutput) = context.Console.Write x

  let! maybeLock = Tasks.readLockIfPresent

  let resolveStart = DateTime.Now
  (text "Resolve start: ") + (string resolveStart |> text |> foreground ConsoleColor.Cyan) |> log

  let! manifest = Tasks.readManifest "."
  "Resolving dependencies... " |> text |> log

  let! resolution = Solver.solve context manifest resolutionStyle maybeLock

  let resolveEnd = DateTime.Now
  (text "Resolve end: ") + (string resolveEnd |> text |> foreground ConsoleColor.Cyan) |> log

  (text "Resolve time: ") + (resolveEnd - resolveStart |> string |> text |> foreground ConsoleColor.Cyan) |> log

  match resolution with
  | Resolution.Failure (package, c, e) ->
    "Error! " |> text |> foreground ConsoleColor.Red |> log
    c.ToString() + " for " + package.ToString() + " coudn't be satisfied because: " + e.Message
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
    do! async {
      try
        let! previousLock = Tasks.readLock
        let diff = Lock.showDiff previousLock lock
        diff |> text |> log
      with _ ->
        ()
    }
    do! Tasks.writeLock lock
    return ()
}
