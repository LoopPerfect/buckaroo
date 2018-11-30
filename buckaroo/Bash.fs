module Buckaroo.Bash

open System
open System.Diagnostics
open FSharp.Control

type BashEvent = 
| Output of string
| Error of string
| Exit of int

let runBash (command : string) = asyncSeq {

  if command.Contains("\"") || command.Contains("$") 
  then 
    return 
      raise <| new Exception("Malicious bash? " + command)

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

  let! exitSignal = 
    p.Exited
    |> Async.AwaitEvent
    |> Async.Ignore 
    |> Async.StartChild

  let mutable buffer = List.empty

  p.OutputDataReceived.AddHandler(new DataReceivedEventHandler(fun _ event -> 
    if event.Data <> null
    then
      buffer <- buffer @ [ Output event.Data ]
  ))

  p.ErrorDataReceived.AddHandler(new DataReceivedEventHandler(fun _ event -> 
    if event.Data <> null
    then
      buffer <- buffer @ [ Error event.Data ]
  ))

  p.Start() |> ignore

  use! cancelHandler = 
    Async.OnCancel (fun () -> 
    (
      p.Kill()
      buffer <- List.empty
    ))

  p.BeginOutputReadLine()
  p.BeginErrorReadLine()

  let flush = asyncSeq {
    for event in buffer do
      yield event
    buffer <- List.empty
  }

  while not p.HasExited do
    yield! flush
  
  do! exitSignal
  yield! flush

  yield Exit p.ExitCode
}

type ProgressCallback = string -> Unit

let escapeBash (command : string) = 
  if command.Contains("\"") || command.Contains("$") 
  then 
    raise <| new Exception("Malicious bash? " + command)
  command

let runBashAsync (command : string) (stdoutHandler : ProgressCallback) (stderrHandler : ProgressCallback) = async {
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

  p.Start() |> ignore

  use! cancelHandler = 
    Async.OnCancel (fun () -> p.Kill())

  p.BeginOutputReadLine()
  p.BeginErrorReadLine()

  do! exitSignal

  return p.ExitCode
}

