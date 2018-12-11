module Buckaroo.Tests.Manifest

open System
open Xunit

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
          Version.SemVerVersion SemVer.zero, {
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
