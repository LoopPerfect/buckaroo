module Buckaroo.Tests.Version

open System
open Xunit

open Buckaroo

[<Fact>]
let ``Version.parse works correctly`` () =
  let cases = [
    ("tag=abc", Version.Tag "abc" |> Result.Ok);
    ("tag=foo/bar", Version.Tag "foo/bar" |> Result.Ok);
    ("tag=v3.0.0", Version.Tag "v3.0.0" |> Result.Ok);
    ("branch=master", Version.Branch "master" |> Result.Ok);
    ("revision=aabbccddee", Version.Revision "aabbccddee" |> Result.Ok);
    ("1.2", Version.SemVerVersion { SemVer.zero with Major = 1; Minor = 2 } |> Result.Ok);
  ]
  for (input, expected) in cases do
    Assert.Equal(expected, Version.parse input)
 
[<Fact>]
let ``Version.harmonious works correctly`` () =
  let cases = [
    (Version.Revision "aabbccddee", Version.Tag "aabbccddee", true);
    (Version.Tag "abc", Version.Tag "abc", true);
    (Version.Branch "master", Version.Branch "master", true);
    (Version.SemVerVersion SemVer.zero, Version.SemVerVersion SemVer.zero, true);
    (Version.SemVerVersion SemVer.zero, Version.Revision "aabbccddee", true);
    (Version.SemVerVersion SemVer.zero, Version.Tag "abc", true);
    (Version.Branch "master", Version.Branch "develop", false);
  ]
  for (v, u, expected) in cases do
    Assert.Equal(expected, Version.harmonious v u)