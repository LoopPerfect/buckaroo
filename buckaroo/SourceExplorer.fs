namespace Buckaroo

open FSharp.Control

type PackageSources = Map<AdhocPackageIdentifier, PackageSource>

type VersionedLock = PackageLock * Set<Version>

type ISourceExplorer =
  abstract member FetchLocations : PackageSources -> PackageIdentifier -> Constraint -> AsyncSeq<PackageLocation * Set<Version>>
  abstract member LockLocation : PackageLocation -> Async<PackageLock>
  abstract member FetchManifest : PackageLock -> Async<Manifest>
  abstract member FetchLock : PackageLock -> Async<Lock>
