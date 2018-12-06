module Buckaroo.Tests.Paths

open Xunit

open Buckaroo
open System

[<Fact>]
let ``Paths.normalize works correctly`` () = 
  let cases = [
    (".", ""); 
    ("a", "a"); 
    ("a/", "a/"); 
    ("/a/", "/a/"); 
    ("a/ /b", "a/ /b"); 
    ("/a/", "/a/"); 
    ("b", "a/../b"); 
    ("c/d", "a/../b/../c/./././d"); 
    ("../../../a/b/c", "../../../a/b/c"); 
  ]

  for (expected, input) in cases do
    Assert.Equal(expected, Paths.normalize input)
