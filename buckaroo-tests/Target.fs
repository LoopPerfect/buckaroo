module Buckaroo.Tests.Target

open System
open Xunit

open Buckaroo

[<Fact>]
let ``Target.parse works correctly`` () =
  let input = "//path/to/some:target"
  let expected = Result.Ok { Folders = [ "path"; "to"; "some" ]; Name = "target" }
  Assert.Equal(expected, Target.parse input)

  let input = "//:foo"
  let expected = Result.Ok { Folders = []; Name = "foo" }
  Assert.Equal(expected, Target.parse input)

  let input = "//foo"
  let expected = Result.Ok { Folders = [ "foo" ]; Name = "foo" }
  Assert.Equal(expected, Target.parse input)

  let input = "//foo/bar"
  let expected = Result.Ok { Folders = [ "foo"; "bar" ]; Name = "bar" }
  Assert.Equal(expected, Target.parse input)

  let input = "//abc/def"
  let expected = Result.Ok { Folders = [ "abc"; "def" ]; Name = "def" }
  Assert.Equal(expected, Target.parse input)

  let input = ":bar"
  let expected = Result.Ok { Folders = []; Name = "bar" }
  Assert.Equal(expected, Target.parse input)

  let input = "foo:bar"
  let expected = Result.Ok { Folders = [ "foo" ]; Name = "bar" }
  Assert.Equal(expected, Target.parse input)

  let input = "//foo_bar:bar_bar"
  let expected = Result.Ok { Folders = [ "foo_bar" ]; Name = "bar_bar" }
  Assert.Equal(expected, Target.parse input)

[<Fact>]
let ``Target.show works correctly`` () =
  let cases =
    [
      "//path/to/some:target"
      "//:foo"
    ]

  for case in cases do
    let actual =
      case
      |> Target.parse
      |> Result.map Target.show

    Assert.Equal(Result.Ok case, actual)
