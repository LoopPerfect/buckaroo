module Buckaroo.UpgradeCommand

open System
open System.IO
open Buckaroo
open Buckaroo.Tasks
open Buckaroo.RichOutput
open Buckaroo.Logger
open Buckaroo

let task context (packages : List<PackageIdentifier>) = async {
  let logger = createLogger context.Console None

  if Seq.isEmpty packages
  then
    logger.Info "Upgrading all packages... "
  else
    logger.Info
      <| "Upgrading [ " + (packages |> Seq.map PackageIdentifier.show |> String.concat " ") + " ]... "

  let extractPartialSolution =
    async {
      if File.Exists (Constants.LockFileName)
      then
        let! lock = Tasks.readLock
        let! partial =
          if packages |> Seq.isEmpty
          then
            async { return Solution.empty }
          else
            async {
              let! solution = Solver.fromLock context.SourceExplorer lock

              return solution
            }

        return partial
      else
        logger.Warning
          "There is no lock-file to upgrade. A fresh lock-file will be generated. "

        return Solution.empty
    }

  let! partial = extractPartialSolution

  let! resolveSucceeded =
    ResolveCommand.task context partial ResolutionStyle.Upgrading

  if resolveSucceeded
  then
    let! install = InstallCommand.task context

    if install <> 0
    then
      logger.Error "Upgraded version(s) were found but the packages could not be installed. "
      return 1
    else
      logger.Success "The upgrade is complete. "
      return 0
  else
    logger.Error "The upgrade failed. No packages were changed. "
    return 1
}
