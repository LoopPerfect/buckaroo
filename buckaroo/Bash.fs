module Buckaroo.Bash

open System
open System.Diagnostics
open FSharp.Control

type BashEvent = 
| Output of string
| Error of string
| Exit of int

let runBash (command : String) = async {
    let timeout = 3 * 60 * 1000
    
    let! task = 
      async {
        if command.Contains("\"") || command.Contains("$") 
        then 
          return 
            raise <| new Exception("Malicious bash? " + command)
        try 

          let startInfo = new ProcessStartInfo()

          startInfo.CreateNoWindow <- true
          startInfo.UseShellExecute <- false
          startInfo.FileName <- "/bin/bash"
          startInfo.Arguments <- "-c \"" + command + "\""
          startInfo.RedirectStandardOutput <- true
          startInfo.RedirectStandardError <- true
          startInfo.RedirectStandardInput <- true
          startInfo.WindowStyle <- ProcessWindowStyle.Hidden

          let p = new Process()

          p.StartInfo <- startInfo
          p.EnableRaisingEvents <- true
          p.Start() |> ignore

          System.Console.WriteLine command

          use reader = p.StandardOutput
          let stdout = reader.ReadToEnd();
          
          use errorReader = p.StandardError
          let stderr = errorReader.ReadToEnd();

          p.WaitForExit()
          if p.ExitCode > 0
          then 
            return 
              raise <| new Exception("Exit code was " + (string p.ExitCode) + "\n" + stderr + "\n command:\n" + command )

          return stdout
        with error -> 
          return raise error
      }
      |> (fun t -> Async.StartChild (t, timeout))
    return! task
  }
