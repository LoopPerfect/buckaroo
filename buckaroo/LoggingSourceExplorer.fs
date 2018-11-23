namespace Buckaroo

open FSharp.Control

type LoggingSourceExplorer (sourceExplorer : ISourceExplorer) = 

  let log (x : string) = System.Console.WriteLine(x)

  interface ISourceExplorer with 

    member this.Prepare location = async {
      log("Preparing " + (PackageLocation.show location) + "... ")
      do! sourceExplorer.Prepare location  
    }

    member this.FetchLocations package version = asyncSeq {
      log("Fetching locations for " + (PackageIdentifier.show package) + "@" + (Version.show version) + "... ")
      yield! sourceExplorer.FetchLocations package version 
    }

    member this.FetchManifest location = async {
      log("Fetching manifest for " + (PackageLocation.show location) + "... ")
      return! sourceExplorer.FetchManifest location
    }

    member this.FetchLock location = async {
      log("Fetching lock for " + (PackageLocation.show location) + "... ")
      return! sourceExplorer.FetchLock location
    }

    member this.FetchVersions package = asyncSeq {
      log("Fetching versions for " + (PackageIdentifier.show package) + "... ")
      yield! sourceExplorer.FetchVersions package
    }
