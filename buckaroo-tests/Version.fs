module Buckaroo.Tests.Version

open System
open Xunit

open Buckaroo

[<Fact>]
let ``Version.parse works correctly`` () =
  let cases = [
    ("tag=abc", Version.Git(GitVersion.Tag "abc") |> Result.Ok);
    ("tag=foo/bar", Version.Git(GitVersion.Tag "foo/bar") |> Result.Ok);
    ("tag=v3.0.0", Version.Git(GitVersion.Tag "v3.0.0") |> Result.Ok);
    ("branch=master", Version.Git(GitVersion.Branch "master") |> Result.Ok);
    ("revision=aabbccddee", Version.Git(GitVersion.Revision "aabbccddee") |> Result.Ok);
    ("1.2", Version.SemVer { SemVer.zero with Major = 1; Minor = 2 } |> Result.Ok);
  ]
  for (input, expected) in cases do
    Assert.Equal(expected, Version.parse input)

[<Fact>]
let ``Version.compare works correctly`` () =
  Assert.Equal(-1, Version.compare (Version.Git(GitVersion.Branch "master")) (Version.Git(GitVersion.Branch "develop")))
  Assert.Equal(1, Version.compare (Version.Git(GitVersion.Branch "develop")) (Version.Git(GitVersion.Branch "master")))
  Assert.Equal(0, Version.compare (Version.Git(GitVersion.Branch "master")) (Version.Git(GitVersion.Branch "master")))

  let input = [
    (Version.Git(GitVersion.Branch "master"));
    (Version.Git(GitVersion.Tag "v1.0.0"));
    (Version.Git(GitVersion.Branch "develop"));
    (Version.Git(GitVersion.Revision "aabbccddee"));
  ]

  let expected = [
    (Version.Git(GitVersion.Tag "v1.0.0"));
    (Version.Git(GitVersion.Branch "master"));
    (Version.Git(GitVersion.Branch "develop"));
    (Version.Git(GitVersion.Revision "aabbccddee"));
  ]

  let actual = input |> List.sortWith Version.compare

  Assert.Equal<List<Version>>(expected, actual)
