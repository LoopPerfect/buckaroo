namespace Buckaroo

type LoggingSourceManager (sourceManager : ISourceManager) = 

  let log (x : string) = System.Console.WriteLine(x)

  interface ISourceManager with 

    member this.FetchLocations package version = async {
      log("Fetching locations for " + (PackageIdentifier.show package) + "@" + (Version.show version) + "... ")
      return! sourceManager.FetchLocations package version 
    }

    member this.FetchManifest location = async {
      log("Fetching manifest for " + (PackageLocation.show location) + "... ")
      return! sourceManager.FetchManifest location
    }

    member this.FetchVersions package = async {
      log("Fetching versions for " + (PackageIdentifier.show package) + "... ")
      return! sourceManager.FetchVersions package
    }
