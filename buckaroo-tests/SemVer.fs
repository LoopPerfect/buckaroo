module Buckaroo.Tests.SemVer

open System
open Xunit

open Buckaroo

let dropError<'T, 'E> (x : Result<'T, 'E>) =
  match x with
  | Result.Ok o -> Some o
  | Result.Error _ -> None

[<Fact>]
let ``SemVer.compare works correctly`` () =
  Assert.True(SemVer.compare SemVer.zero SemVer.zero = 0)
  Assert.True(SemVer.compare { SemVer.zero with Major = 1 } SemVer.zero = 1)
  Assert.True(SemVer.compare { SemVer.zero with Major = 1; Minor = 2 } { SemVer.zero with Major = 1 } = 1)

[<Fact>]
let ``SemVer.parse works correctly`` () =
  let cases = [
    ("7", Some { SemVer.zero with Major = 7 });
    ("6.4", Some { SemVer.zero with Major = 6; Minor = 4 });
    ("1.2.3", Some { SemVer.zero with Major = 1; Minor = 2; Patch = 3 });
    ("  1.2.3  ", Some { SemVer.zero with Major = 1; Minor = 2; Patch = 3 });
    (" 4.5.6.78", Some { Major = 4; Minor = 5; Patch = 6; Increment = 78 });
    ("v1.2.3", Some { SemVer.zero with Major = 1; Minor = 2; Patch = 3 });
    ("V4.2.7", Some { SemVer.zero with Major = 4; Minor = 2; Patch = 7 });
    ("", None);
    ("abc", None);
    ("v0.9.0-g++-4.9", None);
    ("boost-1.66.0", Some { SemVer.zero with Major = 1; Minor = 66 });
    ("boost-1.64.0-beta2", None);
  ]
  for (input, expected) in cases do
    Assert.Equal(expected, SemVer.parse input |> dropError)
