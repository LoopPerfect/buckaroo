module Buckaroo.Tests.Git

open Xunit
open Buckaroo

[<Fact>]
let ``Git.parseBranchOrTag works correctly`` () = 
  let cases = [
    (true, "abc"); 
    (true, "abc/def"); 
    (true, "abc-def"); 
    (false, ""); 
    (false, "abc..def"); 
  ]

  for (expected, input) in cases do
    Assert.Equal(expected, input |> Git.parseBranchOrTag |> Result.isOk)
