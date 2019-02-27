module Buckaroo.Tests.PackageIdentifier

open Xunit
open Buckaroo

[<Fact>]
let ``PackageIdentifier.parse works correctly`` () =
  let cases = [
    ("github.com/abc/def", PackageIdentifier.GitHub { Owner = "abc"; Project = "def" });
    ("github.com/abc/def.ghi", PackageIdentifier.GitHub { Owner = "abc"; Project = "def.ghi" });
    ("github+abc/def", PackageIdentifier.GitHub { Owner = "abc"; Project = "def" });
    ("github+abc/def_ghi", PackageIdentifier.GitHub { Owner = "abc"; Project = "def_ghi" });
    ("bitbucket.org/abc/def", PackageIdentifier.BitBucket { Owner = "abc"; Project = "def" });
    ("gitlab.com/abc/def", PackageIdentifier.GitLab { Groups = [ "abc" ]; Project = "def" });
    ("gitlab.com/abc-def/xyz", PackageIdentifier.GitLab { Groups = [ "abc-def" ]; Project = "xyz" });
    ("github.com/ABC-DEF/XYZ", PackageIdentifier.GitHub { Owner = "abc-def"; Project = "xyz" });
    ("gitlab.com/ABC-DEF/XYZ", PackageIdentifier.GitLab { Groups = [ "abc-def" ]; Project = "xyz" });
    ("bitbucket.org/ABC-DEF/XYZ", PackageIdentifier.BitBucket { Owner = "abc-def"; Project = "xyz" });
    ("gitlab.com/abc/def/xyz", PackageIdentifier.GitLab { Groups = [ "abc"; "def" ]; Project = "xyz" });
  ]

  for (input, expected) in cases do
    // Parses to what we expect?
    Assert.Equal(Result.Ok expected, PackageIdentifier.parse input)

    // Round-trip via show
    Assert.Equal(Result.Ok expected, expected |> PackageIdentifier.show |> PackageIdentifier.parse)
