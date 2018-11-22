namespace Buckaroo

open System.Collections.Concurrent
open FSharp.Control

type CachedSourceExplorer (sourceExplorer : ISourceExplorer) = 

  let locationsCache = new ConcurrentDictionary<PackageIdentifier * Version, AsyncSeq<PackageLocation>>()
  let versionsCache = new ConcurrentDictionary<PackageIdentifier, AsyncSeq<Buckaroo.Version>>()
  let manifestCache = new ConcurrentDictionary<PackageLocation, Buckaroo.Manifest>()
  let lockCache = new ConcurrentDictionary<PackageLocation, Buckaroo.Lock>()

  interface ISourceExplorer with 

    member this.Prepare dependency = async {
      do! sourceExplorer.Prepare dependency  
    }

    member this.FetchLocations package version = asyncSeq {
      let key = (package, version)
      match locationsCache.TryGetValue(key) with 
      | (true, locations) -> 
        return locations
      | (false, _) -> 
        let locations = 
          sourceExplorer.FetchLocations package version 
          |> AsyncSeq.cache
        locationsCache.TryAdd(key, locations) |> ignore
        yield! locations
    }

    member this.FetchManifest location = async {
      match manifestCache.TryGetValue(location) with
      | (true, manifest) -> 
        return manifest
      | (false, _) -> 
        let! manifest = sourceExplorer.FetchManifest location
        manifestCache.TryAdd(location, manifest) |> ignore
        return manifest
    }

    member this.FetchLock location = async {
      match lockCache.TryGetValue(location) with
      | (true, lock) -> 
        return lock
      | (false, _) -> 
        let! lock = sourceExplorer.FetchLock location
        lockCache.TryAdd(location, lock) |> ignore
        return lock
    }

    member this.FetchVersions package = asyncSeq {
      match versionsCache.TryGetValue(package) with
      | (true, x) -> 
        return x
      | (false, _) -> 
        let versions = 
          sourceExplorer.FetchVersions package
          |> AsyncSeq.cache
        versionsCache.TryAdd(package, versions) |> ignore
        yield! versions
    }
