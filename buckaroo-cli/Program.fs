open System

[<EntryPoint>]
let main argv =
  async {
    let session = Guid.NewGuid () |> string
    let input = argv |> String.concat " "

    let! telemetry =
      Buckaroo.Telemetry.postCommand session input
      |> Async.Catch
      |> Async.Ignore
      |> Async.StartChild

    let! exitCode = async {
      try
        match Buckaroo.Command.parse input with
        | Ok (command, loggingLevel, fetchStyle) ->
          let! exitCode =
            command
            |> Buckaroo.Command.runCommand loggingLevel fetchStyle
          return exitCode
        | Error error ->
          Console.WriteLine error
          return 1
      with error ->
        Console.WriteLine error
        return 1
    }

    do! telemetry
    return exitCode
  }
  |> Async.RunSynchronously
