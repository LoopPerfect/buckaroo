namespace Buckaroo

open System.Collections.Concurrent

type CachedSourceManager (sourceManager : ISourceManager) = 
  let cache = new ConcurrentDictionary<PackageIdentifier, Buckaroo.Version list>()
  

  interface ISourceManager with 

    member this.FetchLocations x v = sourceManager.FetchLocations x v 

    member this.FetchManifest x = sourceManager.FetchManifest x

    member this.FetchVersions package = async {
      match cache.TryGetValue(package) with
      | (true, x) -> 
        return x
      | (false, _) -> 
        let! versions = sourceManager.FetchVersions package
        cache.TryAdd(package, versions) |> ignore
        return versions
    }
