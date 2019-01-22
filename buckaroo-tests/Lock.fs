module Buckaroo.Tests.Lock

open Xunit
open Buckaroo

[<Fact>]
let ``Lock.parse works correctly 1`` () =
  let actual =
    [
      "manifest = \"aabbccddee\"";
    ]
    |> String.concat "\n"
    |> Lock.parse

  let expected = {
    ManifestHash = "aabbccddee";
    Dependencies = Set.empty;
    Packages = Map.empty;
  }

  Assert.Equal(Result.Ok expected, actual)

[<Fact>]
let ``Lock.parse works correctly 2`` () =
  let actual =
    [
      "manifest = \"aabbccddee\"";
      "";
      "[[dependency]]";
      "package = \"abc/def\"";
      "target = \"//:def\"";
      "";
      "[lock.\"abc/def\"]";
      "url = \"https://www.abc.com/def.zip\"";
      "versions = [\"1.2.3\"]";
      "sha256 = \"aabbccddee\"";
    ]
    |> String.concat "\n"
    |> Lock.parse

  let expected = {
    ManifestHash = "aabbccddee";
    Dependencies =
      [
        {
          Package = PackageIdentifier.Adhoc { Owner = "abc"; Project = "def" };
          Target = {
            Folders = [];
            Name = "def";
          }
        }
      ]
      |> Set.ofSeq;
    Packages =
      Map.empty
      |> Map.add
        (PackageIdentifier.Adhoc { Owner = "abc"; Project = "def" })
        {
          Versions = Set [Version.SemVer { SemVer.zero with Major = 1; Minor = 2; Patch = 3 }];
          Location =
            (
              Buckaroo.PackageLock.Http
                (
                  {
                    Url = "https://www.abc.com/def.zip";
                    StripPrefix = None;
                    Type = None;
                  },
                  "aabbccddee"
                )
            );
          PrivatePackages = Map.empty;
        };
  }

  Assert.Equal(Result.Ok expected, actual)

[<Fact>]
let ``Lock.parse works correctly 3`` () =
  let actual =
    [
      "manifest = \"aabbccddee\"";
      "";
      "[[dependency]]";
      "package = \"abc/def\"";
      "target = \"//:def\"";
      "";
      "[lock.\"abc/def\"]";
      "url = \"https://www.abc.com/def.zip\"";
      "versions = [\"1.2.3\"]";
      "sha256 = \"aabbccddee\"";
      "";
      "[lock.\"abc/def\".lock.\"ijk/xyz\"]";
      "url = \"https://www.ijk.com/xyz.zip\"";
      "versions = [\"1\"]";
      "sha256 = \"aabbccddee\"";
      "";
    ]
    |> String.concat "\n"
    |> Lock.parse

  let expected = {
    ManifestHash = "aabbccddee";
    Dependencies =
      [
        {
          Package = PackageIdentifier.Adhoc { Owner = "abc"; Project = "def" };
          Target = {
            Folders = [];
            Name = "def";
          }
        }
      ]
      |> Set.ofSeq;
    Packages =
      Map.empty
      |> Map.add
        (PackageIdentifier.Adhoc { Owner = "abc"; Project = "def" })
        {
          Versions = Set [Version.SemVer { SemVer.zero with Major = 1; Minor = 2; Patch = 3 }];
          Location =
            (
              PackageLock.Http
                (
                  ({
                    Url = "https://www.abc.com/def.zip";
                    StripPrefix = None;
                    Type = None;
                  }),
                  "aabbccddee"
                )
            );
          PrivatePackages =
            Map.empty
            |> Map.add
              (PackageIdentifier.Adhoc { Owner = "ijk"; Project = "xyz" })
              {
                Versions = Set.singleton (Version.SemVer { SemVer.zero with Major = 1; });
                Location =
                  (PackageLock.Http (
                    ({
                      Url = "https://www.ijk.com/xyz.zip";
                      StripPrefix = None;
                      Type = None;
                    }),
                    "aabbccddee"
                  ));
                PrivatePackages = Map.empty;
              };
        };
  }

  Assert.Equal(Result.Ok expected, actual)
