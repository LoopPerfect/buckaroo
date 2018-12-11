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
let ``Version.compare works correctly`` () =
  Assert.Equal(-1, Version.compare (Version.Branch "master") (Version.Branch "develop"))
  Assert.Equal(1, Version.compare (Version.Branch "develop") (Version.Branch "master"))
  Assert.Equal(0, Version.compare (Version.Branch "master") (Version.Branch "master"))

  let input = [ 
    (Version.Branch "master"); 
    (Version.Tag "v1.0.0"); 
    (Version.Branch "develop"); 
    (Version.Revision "aabbccddee"); 
  ]
  
  let expected = [ 
    (Version.Tag "v1.0.0"); 
    (Version.Branch "master"); 
    (Version.Branch "develop"); 
    (Version.Revision "aabbccddee"); 
  ]

  let actual = input |> List.sortWith Version.compare

  Assert.Equal<List<Version>>(expected, actual)
