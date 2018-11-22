namespace Buckaroo

open System.Collections.Concurrent
open FSharp.Control

type CachedSourceManager (sourceManager : ISourceManager) = 

  let locationsCache = new ConcurrentDictionary<PackageIdentifier * Version, AsyncSeq<PackageLocation>>()
  let versionsCache = new ConcurrentDictionary<PackageIdentifier, AsyncSeq<Buckaroo.Version>>()
  let manifestCache = new ConcurrentDictionary<PackageLocation, Buckaroo.Manifest>()

  interface ISourceManager with 

    member this.Prepare package = async {
      do! sourceManager.Prepare package  
    }

    member this.FetchLocations package version = asyncSeq {
      let key = (package, version)
      match locationsCache.TryGetValue(key) with 
      | (true, locations) -> 
        return locations
      | (false, _) -> 
        let locations = 
          sourceManager.FetchLocations package version 
          |> AsyncSeq.cache
        locationsCache.TryAdd(key, locations) |> ignore
        yield! locations
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

    member this.FetchVersions package = asyncSeq {
      match versionsCache.TryGetValue(package) with
      | (true, x) -> 
        return x
      | (false, _) -> 
        let versions = 
          sourceManager.FetchVersions package
          |> AsyncSeq.cache
        versionsCache.TryAdd(package, versions) |> ignore
        yield! versions
    }
