namespace Buckaroo

open System.Collections.Concurrent

type CachedSourceManager (sourceManager : ISourceManager) = 

  let locationsCache = new ConcurrentDictionary<PackageIdentifier * Version, PackageLocation list>();
  let versionsCache = new ConcurrentDictionary<PackageIdentifier, Buckaroo.Version list>()
  let manifestCache = new ConcurrentDictionary<PackageLocation, Buckaroo.Manifest>()
  

  interface ISourceManager with 

    member this.FetchLocations package version = async {
      let key = (package, version)
      match locationsCache.TryGetValue(key) with 
      | (true, locations) -> 
        return locations
      | (false, _) -> 
        let! locations = sourceManager.FetchLocations package version 
        locationsCache.TryAdd(key, locations) |> ignore
        return locations
    }

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
