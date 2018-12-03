module Buckaroo.Tests.Glob

open Xunit
open Buckaroo

[<Fact>]
let ``Glob.isLike works correctly`` () =
  Assert.True("a.txt" |> Glob.isLike "**/*")
  Assert.True("a/b.txt" |> Glob.isLike "**/*")
  Assert.True("a/b/c.txt" |> Glob.isLike "**/*")
  Assert.True("boost-array-2618e0d5bbb70ddcd68daa285898be88ddbae714" |> Glob.isLike "*")
  Assert.True("boost-array-2618e0d5bbb70ddcd68daa285898be88ddbae714/" |> Glob.isLike "*")
