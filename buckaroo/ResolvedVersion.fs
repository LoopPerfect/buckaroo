namespace Buckaroo

type ResolvedVersion = {
  Versions : Set<Version>;
  Location : PackageLocation;
  Manifest : Manifest;
}

module ResolvedVersion =

  let isCompatible (x : ResolvedVersion) (y : ResolvedVersion) : bool =
    x.Location = y.Location || x.Versions = y.Versions

  let show x =
    "ResolvedVersion {\n" +
    "  Versions = " +
    (x.Versions
      |> Set.toSeq
      |> Seq.map Version.show
      |> String.concat ", "
      |> (fun x -> "{ " + x + "}")) +
    "\n  Location = " + PackageLocation.show x.Location
