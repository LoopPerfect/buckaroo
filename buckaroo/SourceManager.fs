namespace Buckaroo

type ISourceManager =
  abstract member FetchVersions : PackageIdentifier -> Async<Buckaroo.Version list>
  abstract member FetchLocations : PackageIdentifier -> Buckaroo.Version -> Async<PackageLocation list>
  abstract member FetchManifest : PackageLocation -> Async<Manifest>

// module SourceManager = 
  
  // let cached (sourceManager : SourceManager) = 

  //   let cache = new ConcurrentDictionary<PackageIdentifier, Buckaroo.Version list>()
    
  //   let cachedFetchVersions (project : PackageIdentifier) = async {
  //     match cache.TryGetValue(project) with
  //     | (true, x) -> 
  //       return x
  //     | (false, _) -> 
  //       let! versions = sourceManager.FetchVersions project
  //       cache.TryAdd(project, versions) |> ignore
  //       return versions
  //   }

  //   {
  //     sourceManager with 
  //       FetchVersions = cachedFetchVersions;
  //   }
