module Buckaroo.Logger

open System
open Buckaroo.Console
open Buckaroo.RichOutput

type Logger =
  {
    Print : string -> Unit
    Info : string -> Unit
    RichInfo : RichOutput -> Unit
    Success : string -> Unit
    RichSuccess : RichOutput -> Unit
    Trace : string -> Unit
    Warning : string -> Unit
    RichWarning : RichOutput -> Unit
    Error : string -> Unit
    RichError : RichOutput -> Unit
  }

let createLogger (console : ConsoleManager) (componentName : string option) =
  let componentPrefix =
    componentName
    |> Option.map (fun x -> "[" + x + "] " |> text |> foreground ConsoleColor.DarkGray)
    |> Option.defaultValue (text "")

  let print (x : string) =
    console.Write (componentPrefix + x, LoggingLevel.Info)

  let prefix =
    "info "
    |> text
    |> foreground ConsoleColor.Blue

  let info (x : string) =
    console.Write (componentPrefix + prefix + x, LoggingLevel.Info)

  let richInfo (x : RichOutput) =
    console.Write (componentPrefix + prefix + x, LoggingLevel.Info)

  let prefix =
    "success "
    |> text
    |> foreground ConsoleColor.Green

  let success (x : string) =
    console.Write (componentPrefix + prefix + x, LoggingLevel.Info)

  let richSuccess (x : RichOutput) =
    console.Write (componentPrefix + prefix + x, LoggingLevel.Info)

  let trace (x : string) =
    console.Write (componentPrefix + x, LoggingLevel.Trace)

  let prefix =
    "warning "
    |> text
    |> foreground ConsoleColor.Yellow

  let warning (x : string) =
    console.Write (componentPrefix + prefix + x, LoggingLevel.Info)

  let richWarning (x : RichOutput) =
    console.Write (componentPrefix + prefix + x, LoggingLevel.Info)

  let prefix =
    "error "
    |> text
    |> foreground ConsoleColor.Red

  let error (x : string) =
    console.Write (componentPrefix + prefix + x, LoggingLevel.Info)

  let richError (x : RichOutput) =
    console.Write (componentPrefix + prefix + x, LoggingLevel.Info)

  {
    Print = print
    Info = info
    RichInfo = richInfo
    Success = success
    RichSuccess = richSuccess
    Trace = trace
    Warning = warning
    RichWarning = richWarning
    Error = error
    RichError = richError
  }
