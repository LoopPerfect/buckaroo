module Buckaroo.StartCommand

open Buckaroo.Tasks
open Buckaroo.RichOutput
open System

let task (context : TaskContext) = async {
  let log (x : string) = 
    let rt = 
      x
      |> text 
      |> foreground ConsoleColor.Green 
    context.Console.Write(rt)

  let logUrl (x : string) = 
    x 
    |> text 
    |> foreground ConsoleColor.Cyan
    |> background ConsoleColor.Black
    |> context.Console.Write


  log("   ___           __                             ")
  log("  / _ )__ ______/ /_____ ________  ___          ")
  log(" / _  / // / __/  '_/ _ `/ __/ _ \\/ _ \\       ")
  log("/____/\\_,_/\\__/_/\\_\\\\_,_/_/  \\___/\\___/")
  log("")
  log("Buck, Buck, Buckaroo! \uD83E\uDD20")
  
  logUrl "https://buckaroo.readthedocs.io/"

  do! context.Console.Flush()
}
