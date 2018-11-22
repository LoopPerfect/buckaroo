namespace Buckaroo

open FSharp.Control

type ISourceManager =
  abstract member Prepare : PackageIdentifier -> Async<Unit>
  abstract member FetchVersions : PackageIdentifier -> AsyncSeq<Buckaroo.Version>
  abstract member FetchLocations : PackageIdentifier -> Buckaroo.Version -> AsyncSeq<PackageLocation>
  abstract member FetchManifest : PackageLocation -> Async<Manifest>
