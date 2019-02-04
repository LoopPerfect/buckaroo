module Buckaroo.Bash

open System
open System.Diagnostics
open System.Threading.Tasks

type ProgressCallback = string -> Unit

type BashException(command, exitCode) =
  inherit Exception("The command \"" + command + "\" exited with code " + (string exitCode))
  member this.Command = command
  member this.ExitCode = exitCode

let escapeBash (command : string) =
  if command.Contains("\"") || command.Contains("$")
  then
    raise <| new Exception("Malicious bash? " + command)
  command

let runBashSync (exe : String) (args : String) (stdoutHandler : ProgressCallback) (stderrHandler : ProgressCallback) = async {
  let startInfo = ProcessStartInfo()

  startInfo.CreateNoWindow <- true
  startInfo.UseShellExecute <- false
  startInfo.FileName <- exe
  startInfo.Arguments <- args
  startInfo.RedirectStandardOutput <- true
  startInfo.RedirectStandardError <- true
  startInfo.RedirectStandardInput <- true
  startInfo.WindowStyle <- ProcessWindowStyle.Hidden

  use p = new Process()

  p.StartInfo <- startInfo

  let startProcess () =
    p.OutputDataReceived.AddHandler(new DataReceivedEventHandler(fun _ event ->
      if event.Data <> null
      then
        stdoutHandler (event.Data + System.Environment.NewLine)
    ))

    p.ErrorDataReceived.AddHandler(new DataReceivedEventHandler(fun _ event ->
      if event.Data <> null
      then
        stderrHandler (event.Data + System.Environment.NewLine)
    ))

    p.Start() |> ignore

    p.BeginOutputReadLine()
    p.BeginErrorReadLine()

    p.WaitForExit()

    p.CancelOutputRead()
    p.CancelErrorRead()

  let! exitSignal =
    Task.Factory.StartNew(startProcess)
    |> Async.AwaitTask
    |> Async.StartChild

  do! exitSignal

  if p.ExitCode > 0
  then
    return
      raise <| new BashException(exe + " " + args, p.ExitCode)

  return p.ExitCode
}

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

  use p = new Process()

  p.StartInfo <- startInfo
  p.EnableRaisingEvents <- true

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

  p.Start() |> ignore

  p.BeginOutputReadLine()
  p.BeginErrorReadLine()

  let! task =
    p.Exited
    |> Async.AwaitEvent
    |> Async.Ignore
    |> Async.StartChild

  do! task

  p.CancelOutputRead()
  p.CancelErrorRead()

  return p.ExitCode
}
