module Tests

open System
open Xunit

[<Fact>]
let ``SemVer.parse works correctly`` () =
  Assert.True(SemVer.parse("1.2.3") = Some { SemVer.zero with Major = 1; Minor = 2; Patch = 3 })
  Assert.True(SemVer.parse("  1.2.3  ") = Some { SemVer.zero with Major = 1; Minor = 2; Patch = 3 })
  Assert.True(SemVer.parse("4.5.6.78") = Some { Major = 4; Minor = 5; Patch = 6; Increment = 78 })
  Assert.True(SemVer.parse("") = None)
  Assert.True(SemVer.parse("abc") = None)
