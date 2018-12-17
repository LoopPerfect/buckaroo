module Buckaroo.Console

open System
open Buckaroo.RichOutput

type LoggingLevel = 
| Trace
| Debug
| Info

type OutputCategory = 
| Normal
| Warning
| Error

let private readPassword () = 
  let rec loop password = 
    let key = Console.ReadKey(true)
    if key.Key <> ConsoleKey.Backspace && key.Key <> ConsoleKey.Enter
    then
      Console.Write("*")
      loop (password + (string key.KeyChar))
    else
      if key.Key = ConsoleKey.Backspace && password.Length > 0
      then
        Console.Write("\b \b")
        loop (password.Substring(0, (password.Length - 1)))
      else 
        if key.Key = ConsoleKey.Enter
        then
          password
        else
          loop password
  loop ""

let private renderRichOutput (xs : RichOutput) = 
  let mutable previousForeground = Option.None
  let mutable previousBackground = Option.None

  for x in xs.Segments do
    match (x.Foreground, x.Foreground = previousForeground) with
    | (Some c, false) -> 
      Console.ForegroundColor <- c
      previousForeground <- x.Foreground
    | _ -> ()

    match (x.Background, x.Background = previousBackground) with
    | (Some c, false) -> 
      Console.BackgroundColor <- c
      previousBackground <- x.Background
    | _ -> ()

    Console.WriteLine x.Text

  Console.ResetColor()

type ConsoleMessage = 
| Output of string * LoggingLevel * OutputCategory
| RichOutput of RichOutput * LoggingLevel * OutputCategory
| Input of AsyncReplyChannel<string>
| InputSecret of AsyncReplyChannel<string>
| Flush of AsyncReplyChannel<unit>

type ConsoleManager (minimumLoggingLevel : LoggingLevel) = 
  
  let actor = MailboxProcessor.Start(fun inbox -> async {
    while true do
      let! message = inbox.Receive()
      match message with
      | Output (m, l, c) -> 
        // TODO: Respect minimum logging level
        match c with
        | Normal -> Console.Out.WriteLine(m)
        | Warning -> Console.Out.WriteLine(m)
        | Error -> Console.Error.WriteLine(m) 
        ()
      | RichOutput (m, l, c) -> 
        // TODO: Respect minimum logging level
        match c with
        | Normal -> renderRichOutput m 
        | Warning -> renderRichOutput m 
        | Error -> renderRichOutput m 
        ()
      | Input channel -> 
        let response = Console.ReadLine()
        channel.Reply(response)
        ()
      | InputSecret channel -> 
        let secret = readPassword()
        channel.Reply(secret)
        ()
      | Flush channel -> 
        do! Console.Out.FlushAsync() |> Async.AwaitTask
        do! Console.Error.FlushAsync() |> Async.AwaitTask
        channel.Reply()
        ()
      ()
  })

  member this.Write (message, loggingLevel) = 
    actor.Post (Output (message, loggingLevel, OutputCategory.Normal))

  member this.Write (message : string) = 
    this.Write(message, LoggingLevel.Info)

  member this.Write (message, loggingLevel) = 
    actor.Post (RichOutput (message, loggingLevel, OutputCategory.Normal))

  member this.Write (message) = 
    actor.Post (RichOutput (message, LoggingLevel.Info, OutputCategory.Normal))

  member this.Error (message, loggingLevel) = 
    actor.Post (Output (message, loggingLevel, OutputCategory.Error))

  member this.Read() = 
    actor.PostAndAsyncReply(fun channel -> Input channel)

  member this.ReadSecret() = 
    actor.PostAndAsyncReply(fun channel -> InputSecret channel)

  member this.Flush() = 
    actor.PostAndAsyncReply(fun channel -> Flush channel)
