open System

[<EntryPoint>]
let main argv =
  try
    let input = argv |> String.concat " "
    match Buckaroo.Command.parse input with 
    | Result.Ok command -> 
      command 
      |> Buckaroo.Command.runCommand 
      |> Async.RunSynchronously
      0
    | Result.Error error -> 
      Console.WriteLine error
      1
  with error -> 
    Console.WriteLine error
    1
