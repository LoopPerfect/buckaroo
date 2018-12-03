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
    ("github+abc/def", PackageIdentifier.GitHub { Owner = "abc"; Project = "def" } |> Some);
    ("github+abc/def_ghi", PackageIdentifier.GitHub { Owner = "abc"; Project = "def_ghi" } |> Some);
    ("", None)
  ]
  for (input, expected) in cases do
    Assert.Equal(expected, PackageIdentifier.parse input |> dropError)
