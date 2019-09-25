namespace Buckaroo

type OverrideSourceExplorer (sourceExplorer : ISourceExplorer, overrides : Map<PackageIdentifier, PackageIdentifier>) =

  interface ISourceExplorer with

    member this.FetchVersions locations package =
      let packageAfterOverrides =
        overrides
        |> Map.tryFind package
        |> Option.defaultValue package
      sourceExplorer.FetchVersions locations packageAfterOverrides

    member this.LockLocation packageLocation =
      sourceExplorer.LockLocation packageLocation

    member this.FetchLocations locations package version =
      let packageAfterOverrides =
        overrides
        |> Map.tryFind package
        |> Option.defaultValue package
      sourceExplorer.FetchLocations locations packageAfterOverrides version

    member this.FetchManifest (location, versions) =
      sourceExplorer.FetchManifest (location, versions)

    member this.FetchLock (location, versions) =
      sourceExplorer.FetchLock (location, versions)
