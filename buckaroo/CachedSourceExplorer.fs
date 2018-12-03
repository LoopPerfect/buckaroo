namespace Buckaroo

open System.Collections.Concurrent
open FSharpx.Control

type CachedSourceExplorer (sourceExplorer : ISourceExplorer) = 

  let manifestCache = new ConcurrentDictionary<PackageLocation, Buckaroo.Manifest>()
  let lockCache = new ConcurrentDictionary<PackageLocation, Buckaroo.Lock>()

  interface ISourceExplorer with 

    member this.FetchLocations locations package version = 
      sourceExplorer.FetchLocations locations package version 

    member this.FetchManifest location = async {
      match manifestCache.TryGetValue(location) with
      | (true, manifest) -> 
        return manifest
      | (false, _) -> 
        let! manifest = 
          sourceExplorer.FetchManifest location
          |> Async.Cache
        manifestCache.TryAdd(location, manifest) |> ignore
        return manifest
    }

    member this.FetchLock location = async {
      match lockCache.TryGetValue(location) with
      | (true, lock) -> 
        return lock
      | (false, _) -> 
        let! lock = 
          sourceExplorer.FetchLock location
          |> Async.Cache
        lockCache.TryAdd(location, lock) |> ignore
        return lock
    }

    member this.FetchVersions locations package = 
      sourceExplorer.FetchVersions locations package
