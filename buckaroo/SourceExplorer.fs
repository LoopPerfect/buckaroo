namespace Buckaroo

open FSharp.Control

type ISourceExplorer =
  abstract member Prepare : PackageLocation -> Async<Unit>
  abstract member FetchVersions : PackageIdentifier -> AsyncSeq<Buckaroo.Version>
  abstract member FetchLocations : PackageIdentifier -> Buckaroo.Version -> AsyncSeq<PackageLocation>
  abstract member FetchManifest : PackageLocation -> Async<Manifest>
  abstract member FetchLock : PackageLocation -> Async<Lock>
