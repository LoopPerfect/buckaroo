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
  ]

  for (input, expected) in cases do
    Assert.Equal(Result.Ok expected, PackageIdentifier.parse input)
