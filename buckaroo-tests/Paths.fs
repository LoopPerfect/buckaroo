module Buckaroo.Tests.Paths

open Xunit
open Buckaroo

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
    ("../../../a", "../../.././a"); 
    (
      "../../../../../../buckaroo/github/buckaroo-pm/pkg-config-cairo", 
      "../../../../../.././buckaroo/github/buckaroo-pm/pkg-config-cairo"
    ); 
  ]

  for (expected, input) in cases do
    Assert.Equal(expected, Paths.normalize input)
