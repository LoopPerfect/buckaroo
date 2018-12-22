namespace Buckaroo

open FSharp.Control
open Buckaroo.Console

type LoggingSourceExplorer (console : ConsoleManager, sourceExplorer : ISourceExplorer) =

  let log (x : string) = console.Write x

  interface ISourceExplorer with
    member this.FetchManifest location = async {
      log("Fetching manifest for " + (PackageLock.show location) + "... ")
      return! sourceExplorer.FetchManifest location
    }

    member this.FetchLock location = async {
      log("Fetching lock for " + (PackageLock.show location) + "... ")
      return! sourceExplorer.FetchLock location
    }


    member this.FetchVersions locations package =
      sourceExplorer.FetchVersions locations package

    member this.LockLocation source =
      sourceExplorer.LockLocation source
