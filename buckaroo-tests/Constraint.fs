module Buckaroo.Tests.Constraint

open System
open Xunit

open Buckaroo

let dropError<'T, 'E> (x : Result<'T, 'E>) =
  match x with
  | Result.Ok o -> Some o
  | Result.Error _ -> None

[<Fact>]
let ``Constraint.parse works correctly`` () =
  let cases = [
    ("*", Constraint.wildcard |> Some);
    ("revision=aabbccddee",  Version.Git(GitVersion.Revision "aabbccddee") |> Exactly |> Some);
    ("!*", Constraint.wildcard |> Constraint.Complement |> Some);
    ("any(branch=master)", Some(Any [Exactly (Version.Git(GitVersion.Branch "master"))]));
    ("any(any(branch=master))", Some(Any [ Any [Exactly (Version.Git(GitVersion.Branch "master"))]]));
    ("any(revision=aabbccddee branch=master)", Some(Any [Exactly (Version.Git(GitVersion.Revision "aabbccddee")); Exactly (Version.Git(GitVersion.Branch "master"))]));
    ("all(*)", Some(All [Constraint.wildcard]));
    ("", None);
  ]
  for (input, expected) in cases do
    Assert.Equal(expected, Constraint.parse input |> dropError)

[<Fact>]
let ``Constraint.satisfies works correctly`` () =
  let v = Version.Git(GitVersion.Revision "aabbccddee")
  let w = Version.Git(GitVersion.Tag "rc1")
  let c = Constraint.Exactly v
  Assert.True(Constraint.satisfies c (set [ v ]))
  Assert.False(Constraint.satisfies c (set [ w ]))

[<Fact>]
let ``Constraint.agreesWith works correctly`` () =
  let v = Version.Git(GitVersion.Revision "aabbccddee")
  let w = Version.Git(GitVersion.Tag "rc1")
  let x = Version.Git(GitVersion.Revision "ffgghhiijjkk")
  let c = Constraint.Exactly v
  Assert.True(Constraint.agreesWith c v)
  Assert.True(Constraint.agreesWith c w)
  Assert.False(Constraint.agreesWith c x)

[<Fact>]
let ``Constraint.compare works correctly`` () =
  let input = [
    (Constraint.Exactly <| Version.Git(GitVersion.Branch "master"));
    (Constraint.Exactly <| Version.Git(GitVersion.Tag "v1.0.0"));
    (Constraint.wildcard);
    (Constraint.Exactly <| Version.Git(GitVersion.Revision "aabbccddee"));
  ]
  let expected = [
    (Constraint.Exactly <| Version.Git(GitVersion.Revision "aabbccddee"));
    (Constraint.Exactly <| Version.Git(GitVersion.Tag "v1.0.0"));
    (Constraint.Exactly <| Version.Git(GitVersion.Branch "master"));
    (Constraint.wildcard);
  ]
  let actual = input |> List.sortWith Constraint.compare
  Assert.Equal<List<Constraint>>(expected, actual)

[<Fact>]
let ``Constraint.simplify works correctly`` () =
  let cases = [
    ("!!revision=aabbccddee", "revision=aabbccddee");
    ("any(any(revision=aabbccddee))", "revision=aabbccddee");
    ("all(all(revision=aabbccddee))", "revision=aabbccddee");
    ("any(all(revision=aabbccddee))", "revision=aabbccddee");
    ("all(any(revision=aabbccddee))", "revision=aabbccddee");
    ("any(branch=master any(revision=aabbccddee))", "any(revision=aabbccddee branch=master)");
  ]
  for (input, expected) in cases do
    let actual =
      Constraint.parse input
      |> Result.map Constraint.simplify
    Assert.Equal(Constraint.parse expected, actual)
