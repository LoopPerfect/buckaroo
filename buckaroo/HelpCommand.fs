module Buckaroo.HelpCommand

open Buckaroo.Tasks
open Buckaroo.RichOutput
open System

let task (context : TaskContext) = async {
  let log (x : string) = context.Console.Write(x)

  let logBash (x : string) =
    x
    |> text
    |> foreground ConsoleColor.Green
    |> context.Console.Write

  let logUrl (x : string) =
    x
    |> text
    |> foreground ConsoleColor.Cyan
    |> background ConsoleColor.Black
    |> context.Console.Write

  logBash("$ buckaroo init")
  log("Create a Buckaroo manifest in the current working directory. ")
  log("")

  logBash("$ buckaroo resolve")
  log("Generates a fresh lock-file from the existing manifest. ")
  log("")

  logBash("$ buckaroo install")
  log("Installs the packages as described in the current lock-file. ")
  log("")

  logBash("$ buckaroo add <package>@<version>...")
  log("Adds the given package(s) to the current manifest, updates the lock-file and installs it to the packages folder. ")
  log("If no satisfactory resolution can be found then nothing is changed. ")
  log("")

  logBash("$ buckaroo upgrade [ <package> [ @<version> ] ]")
  log("Upgrades the given package(s) to the highest version that meets the constraints in the manifest. ")
  log("Optionally, a version can be specified to move the package to. ")
  log("If no packages are specified, then all are upgraded. ")
  log("")

  logBash("$ buckaroo remove <package>...")
  log("Removes an existing package from the manifest, updates the lock-file and deletes it from the packages folder. ")
  log("If no satisfactory resolution can be found then nothing is changed. ")
  log("")

  logBash("$ buckaroo version")
  log("Displays the version of this installation of Buckaroo. ")
  log("")

  logBash("$ buckaroo help")
  log("Displays this message. ")
  log("")

  log("For more information, visit: ")
  logUrl("https://github.com/LoopPerfect/buckaroo")

  do! context.Console.Flush()

  return 0
}
