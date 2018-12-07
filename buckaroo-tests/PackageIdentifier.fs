module Buckaroo.Tests.PackageIdentifier

open System
open Xunit

open Buckaroo

let dropError<'T, 'E> (x : Result<'T, 'E>) =
  match x with 
  | Result.Ok o -> Some o
  | Result.Error _ -> None

[<Fact>]
let ``PackageIdentifier.parse works correctly`` () = 
  let cases = [
    ("github.com/abc/def", PackageIdentifier.GitHub { Owner = "abc"; Project = "def" } |> Some);
    ("github.com/abc/def.ghi", PackageIdentifier.GitHub { Owner = "abc"; Project = "def.ghi" } |> Some);
    ("gh+abc/def", PackageIdentifier.GitHub { Owner = "abc"; Project = "def" } |> Some);
    ("gh+abc/def_ghi", PackageIdentifier.GitHub { Owner = "abc"; Project = "def_ghi" } |> Some);
    ("git://user@test.com:1337/path/to.git", 
      PackageIdentifier.Git { Protocol = "git"; Uri = "user@test.com:1337/path/to.git" } |> Some);
    ("git+ssh://user@test.com:1337/path/to.git", 
      PackageIdentifier.Git { Protocol = "ssh"; Uri = "user@test.com:1337/path/to.git" } |> Some);
    ("git+file://./relative/path", 
      PackageIdentifier.Git { Protocol = "file"; Uri = "./relative/path" } |> Some);
    ("git+http://user:password@test.com/user/project.git", 
      PackageIdentifier.Git { Protocol = "http"; Uri = "user:password@test.com/user/project.git" } |> Some);
    ("", None)
  ]
  for (input, expected) in cases do
    Assert.Equal(expected, PackageIdentifier.parse input |> dropError)
