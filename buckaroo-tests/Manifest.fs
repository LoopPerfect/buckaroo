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
  let cases = 
    [
      ("", Result.Ok Manifest.zero);
      ("[[dependency]]\npackage = \"github.com/abc/def\"\nversion = \"*\"", Result.Ok { Manifest.zero with Dependencies = set [ a ] });
      ("[[dependency]]\npackage = \"github.com/ijk/xyz\"\nversion = \"*\"\ntargets = [ \"//:foo\" ]", Result.Ok { Manifest.zero with Dependencies = set [ b ] });
    ]
  for (input, expected) in cases do
    Assert.Equal(expected, Manifest.parse input)
