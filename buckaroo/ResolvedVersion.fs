namespace Buckaroo

type ResolvedVersion = {
  Version : Version;
  Location : PackageLocation;
  Manifest : Manifest;
}

module ResolvedVersion =

  let isCompatible (x : ResolvedVersion) (y : ResolvedVersion) : bool =
    x.Location = y.Location || x.Version = y.Version

  let show x =
    Version.show x.Version + "(" + PackageLocation.show x.Location + ")"
