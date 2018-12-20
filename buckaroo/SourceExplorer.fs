namespace Buckaroo

open FSharp.Control

type PackageSources = Map<AdhocPackageIdentifier, PackageSource>

type VersionedSource =
| Git of PackageLocation * Set<Version>
| Http of HttpPackageSource * Set<Version>

module VersionedSource =
  let getVersionSet = function
  | VersionedSource.Git (_, vs) -> vs
  | VersionedSource.Http (_, vs) -> vs

type VersionedLocation = PackageLocation * Set<Version>

type ISourceExplorer =
  abstract member FetchVersions : PackageSources -> PackageIdentifier -> AsyncSeq<VersionedSource>
  abstract member FetchLocation : VersionedSource -> Async<VersionedLocation>
  abstract member FetchManifest : PackageLocation -> Async<Manifest>
  abstract member FetchLock : PackageLocation -> Async<Lock>
