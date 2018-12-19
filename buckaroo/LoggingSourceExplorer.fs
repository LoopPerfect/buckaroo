namespace Buckaroo

open FSharp.Control
open Buckaroo.Console

type LoggingSourceExplorer (console : ConsoleManager, sourceExplorer : ISourceExplorer) = 

  let log (x : string) = console.Write x

  interface ISourceExplorer with 

    member this.FetchLocations locations package version = asyncSeq {
      log("Fetching locations for " + (PackageIdentifier.show package) + "@" + (Version.show version) + "... ")
      yield! sourceExplorer.FetchLocations locations package version 
    }

    member this.FetchManifest location = async {
      log("Fetching manifest for " + (PackageLocation.show location) + "... ")
      return! sourceExplorer.FetchManifest location
    }

    member this.FetchLock location = async {
      log("Fetching lock for " + (PackageLocation.show location) + "... ")
      return! sourceExplorer.FetchLock location
    }

    member this.FetchVersions locations package = asyncSeq {
      log("Fetching versions for " + (PackageIdentifier.show package) + "... ")
      yield! sourceExplorer.FetchVersions locations package
    }
