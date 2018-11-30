module Buckaroo.Bash

open System
open System.Diagnostics

let escapeBash (command : string) = 
  if command.Contains("\"") || command.Contains("$") 
  then 
    raise <| new Exception("Malicious bash? " + command)
  command

let runBashSync (command : String) = async {
  let timeout = 3 * 60 * 1000
  
  let! task = 
    async {
      let startInfo = new ProcessStartInfo()

      startInfo.CreateNoWindow <- true
      startInfo.UseShellExecute <- false
      startInfo.FileName <- "/bin/bash"
      startInfo.Arguments <- "-c \"" + (escapeBash command) + "\""
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
    }
    |> (fun t -> Async.StartChild (t, timeout))
  
  return! task
}

type ProgressCallback = string -> Unit

let runBash (command : string) (stdoutHandler : ProgressCallback) (stderrHandler : ProgressCallback) = async {
  let startInfo = new ProcessStartInfo()

  startInfo.CreateNoWindow <- true
  startInfo.UseShellExecute <- false
  startInfo.FileName <- "/bin/bash"
  startInfo.Arguments <- "-c \"" + (escapeBash command) + "\""
  startInfo.RedirectStandardOutput <- true
  startInfo.RedirectStandardError <- true
  startInfo.RedirectStandardInput <- true
  startInfo.WindowStyle <- ProcessWindowStyle.Hidden

  let p = new Process()

  p.StartInfo <- startInfo
  p.EnableRaisingEvents <- true

  let! exitSignal = 
    p.Exited
    |> Async.AwaitEvent
    |> Async.Ignore 
    |> Async.StartChild

  p.OutputDataReceived.AddHandler(new DataReceivedEventHandler(fun _ event -> 
    if event.Data <> null
    then
      stdoutHandler event.Data
  ))

  p.ErrorDataReceived.AddHandler(new DataReceivedEventHandler(fun _ event -> 
    if event.Data <> null
    then
      stderrHandler event.Data
  ))

  System.Console.WriteLine command

  p.Start() |> ignore

  use! cancelHandler = 
    Async.OnCancel (fun () -> p.Kill())

  p.BeginOutputReadLine()
  p.BeginErrorReadLine()

  do! exitSignal

  return p.ExitCode
}
