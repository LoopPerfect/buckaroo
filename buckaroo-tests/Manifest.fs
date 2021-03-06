module Buckaroo.Tests.Manifest

open Xunit
open FSharpx

open Buckaroo
open Buckaroo.Tests

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
                (lmnqrs, locationC)
              ]
          }
      );
    ]

  for (input, expected) in cases do
    let actual = Manifest.parse input

    Assert.Equal(expected, actual)


[<Fact>]
let ``Manifest.toToml roundtrip 1`` () =
  let expected : Manifest = {
    Manifest.zero with
      Targets = Set [
        {Folders=["foo"; "bar"]; Name = "xxx"}
        {Folders=["foo"; "bar"]; Name = "yyy"}
      ]
      Dependencies = Set [{
        Targets = Some ([{Folders=["foo"; "bar"]; Name = "xxx"}])
        Constraint = All <| Set[Constraint.Exactly (Version.SemVer SemVer.zero)]
        Package = PackageIdentifier.GitHub { Owner = "abc"; Project = "def" }
      }]
  }

  let actual =  expected |> Manifest.toToml |> Manifest.parse

  // 3 new-lines indicates poor formatting
  Assert.True (
    expected
    |> Manifest.toToml
    |> String.contains "\n\n\n"
    |> not
  )

  Assert.Equal (Result.Ok expected, actual)

[<Fact>]
let ``Manifest.toToml roundtrip 2`` () =
  let expected : Manifest = {
    Targets =
      Set [
        { Folders = [ "foo"; "bar"]; Name = "xxx" }
        { Folders = [ "foo"; "bar"]; Name = "yyy" }
      ]
    Tags = Set [ "c++"; "java"; "ml" ]
    Locations = Map.ofSeq [
      ({Owner = "testorg1"; Project = "test1"}, PackageSource.Http (Map.ofSeq [
        (Version.SemVer { SemVer.zero with Major = 1 }, {
          Url = "https://test.com"
          StripPrefix = Some "prefix"
          Type = Some ArchiveType.Zip
        })
      ]));
      ({Owner = "testorg2"; Project = "test2"}, PackageSource.Http (Map.ofSeq [
        (Version.SemVer { SemVer.zero with Major = 2 }, {
          Url = "https://testing.com"
          StripPrefix = Some "other_prefix"
          Type = Some ArchiveType.Zip
        })
      ]))
    ]
    Dependencies = Set [{
      Targets = Some ([{Folders=["foo"; "bar"]; Name = "xxx"}])
      Constraint = All <| Set[Constraint.Exactly (Version.SemVer SemVer.zero)]
      Package = PackageIdentifier.GitHub { Owner = "abc"; Project = "def" }
    }]
    PrivateDependencies = Set [{
      Targets = Some ([{Folders=["foo"; "bar"]; Name = "yyy"}])
      Constraint = Any <|Set[Constraint.Exactly (Version.SemVer SemVer.zero)]
      Package = PackageIdentifier.GitHub { Owner = "abc"; Project = "def" }
    }]
    Overrides = Map.empty
  }

  let actual =  expected |> Manifest.toToml |> Manifest.parse

  // 3 new-lines indicates poor formatting
  Assert.True (
    expected
    |> Manifest.toToml
    |> String.contains "\n\n\n"
    |> not
  )

  Assert.Equal (Result.Ok expected, actual)

[<Fact>]
let ``Manifest.toToml roundtrip 3`` () =
  let expected : Manifest = {
    Manifest.zero with
      Overrides =
        Map.empty
        |> Map.add
          (PackageIdentifier.GitHub { Owner = "abc"; Project = "def" })
          (PackageIdentifier.GitHub { Owner = "abc"; Project = "pqr" })
        |> Map.add
          (PackageIdentifier.GitHub { Owner = "ijk"; Project = "lmo" })
          (PackageIdentifier.GitHub { Owner = "gfh"; Project = "xyz" })
  }

  let actual =  expected |> Manifest.toToml |> Manifest.parse

  // 3 new-lines indicates poor formatting
  Assert.True (
    expected
    |> Manifest.toToml
    |> String.contains "\n\n\n"
    |> not
  )

  Assert.Equal (Result.Ok expected, actual)
