open System
open System.IO

[<EntryPoint>]
let main argv =
  let input = argv |> String.concat " "
  match Command.parse input with 
  | Ok command -> command |> Command.runCommand |> Async.RunSynchronously
  | Error error -> Console.WriteLine error
  0
