namespace Buckaroo

open FSharp.Control

type LoggingSourceManager (sourceManager : ISourceManager) = 

  let log (x : string) = System.Console.WriteLine(x)

  interface ISourceManager with 

    member this.Prepare package = async {
      log("Preparing " + (PackageIdentifier.show package) + "... ")
      do! sourceManager.Prepare package  
    }

    member this.FetchLocations package version = asyncSeq {
      log("Fetching locations for " + (PackageIdentifier.show package) + "@" + (Version.show version) + "... ")
      yield! sourceManager.FetchLocations package version 
    }

    member this.FetchManifest location = async {
      log("Fetching manifest for " + (PackageLocation.show location) + "... ")
      return! sourceManager.FetchManifest location
    }

    member this.FetchVersions package = asyncSeq {
      log("Fetching versions for " + (PackageIdentifier.show package) + "... ")
      yield! sourceManager.FetchVersions package
    }
