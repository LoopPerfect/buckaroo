module Buckaroo.Tests.Dependency

open System
open Xunit

open Buckaroo

[<Fact>]
let ``Dependency.parse works correctly`` () =
  let p = PackageIdentifier.GitHub { Owner = "abc"; Project = "def" }
  let cases = [
    ("github.com/abc/def@*", { Package = p; Constraint = Constraint.wildcard; Targets = None; Features = None } |> Result.Ok)
    // TODO: 
    // ("github.com/abc/def@*//:foo", { Package = p; Constraint = Constraint.wildcard; Targets = Some [ { Folders = []; Name = "foo" } ] } |> Result.Ok)
    // ("", Result.Error ""); 
  ]
  for (input, expected) in cases do
    Assert.Equal(expected, Dependency.parse input)
