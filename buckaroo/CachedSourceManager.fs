namespace Buckaroo

open System.Collections.Concurrent

type CachedSourceManager (sourceManager : ISourceManager) = 
  let versionsCache = new ConcurrentDictionary<PackageIdentifier, Buckaroo.Version list>()
  let manifestCache = new ConcurrentDictionary<PackageLocation, Buckaroo.Manifest>()
  

  interface ISourceManager with 

    member this.FetchLocations x v = sourceManager.FetchLocations x v 

    member this.FetchManifest location = async {
      match manifestCache.TryGetValue(location) with
      | (true, manifest) -> 
        return manifest
      | (false, _) -> 
        let! manifest = sourceManager.FetchManifest location
        manifestCache.TryAdd(location, manifest) |> ignore
        return manifest
    }

    member this.FetchVersions package = async {
      match versionsCache.TryGetValue(package) with
      | (true, x) -> 
        return x
      | (false, _) -> 
        let! versions = sourceManager.FetchVersions package
        versionsCache.TryAdd(package, versions) |> ignore
        return versions
    }
