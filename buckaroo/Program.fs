open System
open Buckaroo

[<EntryPoint>]
let main argv =
  let input = argv |> String.concat " "
  match Command.parse input with 
  | Result.Ok command -> command |> Command.runCommand |> Async.RunSynchronously
  | Result.Error error -> Console.WriteLine error
  0
