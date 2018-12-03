module Buckaroo.ResolveCommand

open System

let task (context : Tasks.TaskContext) resolutionStyle = async {
  let sourceExplorer = context.SourceExplorer
  
  let! maybeLock = Tasks.readLockIfPresent

  let resolveStart = DateTime.Now
  "Resolve start: " + (string resolveStart) |> Console.WriteLine

  let! manifest = Tasks.readManifest
  "Resolving dependencies... " |> Console.WriteLine
  let! resolution = Solver.solve sourceExplorer manifest resolutionStyle maybeLock 

  let resolveEnd = DateTime.Now
  "Resolve end: " + (string resolveEnd) |> Console.WriteLine

  "Resolve time: " + (string (resolveEnd - resolveStart)) |> Console.WriteLine

  match resolution with
  | Resolution.Conflict x -> 
    "Conflict! " |> Console.WriteLine
    x |> Console.WriteLine
    return ()
  | Resolution.Error e -> 
    "Error! " |> Console.WriteLine
    e |> Console.WriteLine
    return ()
  | Resolution.Ok solution -> 
    "Success! " |> Console.WriteLine
    let lock = Lock.fromManifestAndSolution manifest solution
    do! async {
      try
        let! previousLock = Tasks.readLock
        let diff = Lock.showDiff previousLock lock 
        Console.WriteLine(diff)
      with _ -> 
        ()
    }
    do! Tasks.writeLock lock
    return ()
}
