namespace Buckaroo

open FSharp.Control

type LoggingSourceExplorer (sourceExplorer : ISourceExplorer) = 

  let log (x : string) = System.Console.WriteLine(x)

  interface ISourceExplorer with 

    member this.FetchLocations package version = asyncSeq {
      log("Fetching locations for " + (PackageIdentifier.show package) + "@" + (Version.show version) + "... ")
      yield! sourceExplorer.FetchLocations package version 
    }

    member this.FetchManifest location branchHint = async {
      log("Fetching manifest for " + (PackageLocation.show location) + "... ")
      return! sourceExplorer.FetchManifest location branchHint
    }

    member this.FetchLock location branchHint = async {
      log("Fetching lock for " + (PackageLocation.show location) + "... ")
      return! sourceExplorer.FetchLock location branchHint
    }

    member this.FetchVersions package = asyncSeq {
      log("Fetching versions for " + (PackageIdentifier.show package) + "... ")
      yield! sourceExplorer.FetchVersions package
    }
