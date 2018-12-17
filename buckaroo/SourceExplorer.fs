namespace Buckaroo

open FSharp.Control

type PackageSources = Map<AdhocPackageIdentifier, PackageSource>

type ISourceExplorer =
  abstract member FetchVersions : PackageSources -> PackageIdentifier -> Constraint -> AsyncSeq<Buckaroo.Version>
  abstract member FetchLocations : PackageSources -> PackageIdentifier -> Buckaroo.Version -> AsyncSeq<PackageLocation>
  abstract member FetchManifest : PackageLocation -> Async<Manifest>
  abstract member FetchLock : PackageLocation -> Async<Lock>
