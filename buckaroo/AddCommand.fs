module Buckaroo.AddCommand

open System
open System.IO
open Buckaroo.RichOutput
open Buckaroo

let task (context : Tasks.TaskContext) dependencies = async {
  context.Console.Write (
    (text "Adding ") + 
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

    let! resolution = Solver.solve context newManifest ResolutionStyle.Quick maybeLock 
    
    match resolution with
    | Resolution.Ok solution -> 
      do! Tasks.writeManifest newManifest
      do! Tasks.writeLock (Lock.fromManifestAndSolution newManifest solution)
      do! InstallCommand.task context
    | _ -> 
      ()
    
  context.Console.Write ("Success. " |> text |> foreground ConsoleColor.Green)
}
