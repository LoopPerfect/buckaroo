module Buckaroo.UpgradeCommand

open System
open System.IO
open Buckaroo
open Buckaroo.Tasks
open Buckaroo.RichOutput

let task context (packages : List<PackageIdentifier>) = async {
  if Seq.isEmpty packages
  then
    context.Console.Write (
      (
        "info "
        |> text
        |> foreground ConsoleColor.Blue
      ) +
      "Upgrading all packages... "
    )
  else
    context.Console.Write (
      (
        "info "
        |> text
        |> foreground ConsoleColor.Blue
      ) +
      "Upgrading [ " + (packages |> Seq.map PackageIdentifier.show |> String.concat " ") + " ]... "
    )

  if File.Exists (Constants.LockFileName)
  then
    let! lock = Tasks.readLock
    let! partial =
      if packages |> Seq.isEmpty
      then async { return Solution.empty }
      else
        async {
          let! solution = Solver.fromLock context.SourceExplorer lock

          return solution
         //   |> Set.ofList
         //   |> Solver.unlock solution
        }

    do! ResolveCommand.task context partial ResolutionStyle.Upgrading
    do! InstallCommand.task context

    return ()
  else
    context.Console.Write (
      (
        "warning "
        |> text
        |> foreground ConsoleColor.Yellow
      ) +
      "There is no lock-file to upgrade. A fresh lock-file will be generated. "
    )

    do! ResolveCommand.task context Solution.empty ResolutionStyle.Upgrading
    do! InstallCommand.task context

    return ()
}