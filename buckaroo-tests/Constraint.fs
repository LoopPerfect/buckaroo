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
    ("any(branch=master)", Some(Any [Exactly (Version.Branch "master")])); 
    ("any(revision=aabbccddee branch=master)", Some(Any [Exactly (Version.Revision "aabbccddee"); Exactly (Version.Branch "master")])); 
    ("all(*)", Some(All [Constraint.wildcard])); 
    ("", None); 
  ]
  for (input, expected) in cases do
    Assert.Equal(expected, Constraint.parse input |> dropError)

[<Fact>]
let ``Constraint.satisfies works correctly`` () =
  let v = Version.Revision "aabbccddee"
  let w = Version.Tag "rc1"
  let c = Constraint.Exactly v
  Assert.True(Constraint.satisfies c v)
  Assert.False(Constraint.satisfies c w)

[<Fact>]
let ``Constraint.agreesWith works correctly`` () =
  let v = Version.Revision "aabbccddee"
  let w = Version.Tag "rc1"
  let x = Version.Revision "ffgghhiijjkk"
  let c = Constraint.Exactly v
  Assert.True(Constraint.agreesWith c v)
  Assert.True(Constraint.agreesWith c w)
  Assert.False(Constraint.agreesWith c x)