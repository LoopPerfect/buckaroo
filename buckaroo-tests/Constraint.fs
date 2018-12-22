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
    ("revision=aabbccddee", "aabbccddee" |> Version.Revision |> Exactly |> Some);
    ("!*", Constraint.wildcard |> Constraint.Complement |> Some);
    ("any(branch=master)", Some (Any [Exactly (Version.Branch "master")]));
    ("any(revision=aabbccddee branch=master)", Some (Any [Exactly (Version.Revision "aabbccddee"); Exactly (Version.Branch "master")]));
    ("all(*)", Some (All [Constraint.wildcard]));
    (
      "all(branch=master !revision=aabbccddee)",
      Some (All [Exactly (Version.Branch "master"); Complement (Exactly (Version.Revision "aabbccddee"))])
    );
    (
      "all(branch=master !any(revision=aabbccddee branch=develop))",
      Some (All [
        Exactly (Version.Branch "master");
        Complement (Any([
          Exactly (Version.Revision "aabbccddee");
          Exactly (Version.Branch "develop");
        ]))
      ])
    );
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
    (Constraint.Exactly <| Version.Branch "master");
    (Constraint.Exactly <| Version.Tag "v1.0.0");
    (Constraint.wildcard);
    (Constraint.Exactly <| Version.Revision "aabbccddee");
  ]
  let expected = [
    (Constraint.Exactly <| Version.Revision "aabbccddee");
    (Constraint.Exactly <| Version.Tag "v1.0.0");
    (Constraint.Exactly <| Version.Branch "master");
    (Constraint.wildcard);
  ]
  let actual = input |> List.sortWith Constraint.compare
  Assert.Equal<List<Constraint>>(expected, actual)
