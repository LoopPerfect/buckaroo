namespace Buckaroo

open FSharp.Control

type ISourceExplorer =
  abstract member FetchVersions : PackageIdentifier -> AsyncSeq<Buckaroo.Version>
  abstract member FetchLocations : PackageIdentifier -> Buckaroo.Version -> AsyncSeq<PackageLocation>
  abstract member FetchManifest : PackageLocation -> string -> Async<Manifest>
  abstract member FetchLock : PackageLocation -> string-> Async<Lock>
