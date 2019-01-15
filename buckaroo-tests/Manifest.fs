module Buckaroo.Tests.Manifest

open System
open Xunit

open Buckaroo
open Buckaroo.Tests
open Buckaroo

[<Fact>]
let ``Manifest.parse works correctly`` () =
  let a = {
    Package = PackageIdentifier.GitHub { Owner = "abc"; Project = "def" };
    Constraint = Constraint.wildcard;
    Targets = None
  }

  let b = {
    Package = PackageIdentifier.GitHub { Owner = "ijk"; Project = "xyz" };
    Constraint = Constraint.wildcard;
    Targets = Some [ { Folders = []; Name = "foo" } ]
  }

  let lmnqrs = { Owner = "lmn"; Project = "qrs" }

  let c = {
    Package = PackageIdentifier.Adhoc lmnqrs;
    Constraint = Constraint.wildcard;
    Targets = None;
  }

  let locationC =
    PackageSource.Http (
      Map.ofSeq [
        (
          Version.SemVer { SemVer.zero with Major = 1 }, {
            Url = "https://lmn/qrs.zip";
            StripPrefix = Some "%";
            Type = None;
          }
        )
      ]
    )

  let cases =
    [
      ("", Result.Ok Manifest.zero);

      (
        "[[dependency]]\npackage = \"github.com/abc/def\"\nversion = \"*\"",
        Result.Ok { Manifest.zero with Dependencies = set [ a ] }
      );

      (
        "[[dependency]]\npackage = \"github.com/ijk/xyz\"\nversion = \"*\"\ntargets = [ \"//:foo\" ]",
        Result.Ok { Manifest.zero with Dependencies = set [ b ] });

      (
        "[[dependency]]\npackage = \"lmn/qrs\"\nversion = \"*\"\n\n" +
        "[[location]]\npackage = \"lmn/qrs\"\nversion = \"1.0.0\"\nurl = \"https://lmn/qrs.zip\"\nstrip_prefix = \"%\"",
        Result.Ok
          {
            Manifest.zero with
              Dependencies = set [ c ];
              Locations = Map.ofSeq [
                (
                  lmnqrs,
                  locationC
                )
              ]
          }
      );
    ]

  for (input, expected) in cases do
    let actual = Manifest.parse input
    Assert.Equal(expected, actual)


[<Fact>]
let ``Manifest.toToml roundtrip`` () =
  let expected : Manifest = {
    Targets = Set [
      {Folders=["foo"; "bar"]; Name = "//xxx"}
      {Folders=["foo"; "bar"]; Name = "//yyy"}
    ]
    Tags = Set ["c++"; "java"; "ml" ]
    Locations = Map.ofSeq [
      ({Owner="Test"; Project="Test1"}, PackageSource.Http (Map.ofSeq [
        (Version.SemVer SemVer.zero, {
          Url = "https://test.com"
          StripPrefix = Some "prefix"
          Type = Some ArchiveType.Zip
        })
      ]))
    ]
    Dependencies = Set [{
      Targets = Some ([])
      Constraint = Constraint.Exactly (Version.SemVer SemVer.zero)
      Package = PackageIdentifier.GitHub { Owner = "abc"; Project = "def" }
    }]
    PrivateDependencies = Set [{
      Targets = Some ([])
      Constraint = Constraint.Exactly (Version.SemVer SemVer.zero)
      Package = PackageIdentifier.GitHub { Owner = "abc"; Project = "def" }
    }]
  }

  let actual = expected |> Manifest.toToml |> Manifest.parse
  System.Console.WriteLine (expected |> Manifest.toToml)
  match actual with
  | Result.Ok o -> System.Console.WriteLine (o.ToString())
  | Result.Error e -> System.Console.WriteLine (e.ToString())
  Assert.Equal(Result.Ok expected, actual)
  ()