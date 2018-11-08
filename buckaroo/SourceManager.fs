namespace Buckaroo

type ISourceManager =
  abstract member FetchVersions : PackageIdentifier -> Async<Buckaroo.Version list>
  abstract member FetchLocations : PackageIdentifier -> Buckaroo.Version -> Async<PackageLocation list>
  abstract member FetchManifest : PackageLocation -> Async<Manifest>
