module Tests

open System
open Xunit

open Manifest

[<Fact>]
let ``SemVer.parse works correctly`` () =
  Assert.True(SemVer.parse("1.2.3") = Some { SemVer.zero with Major = 1; Minor = 2; Patch = 3 })
  Assert.True(SemVer.parse("  1.2.3  ") = Some { SemVer.zero with Major = 1; Minor = 2; Patch = 3 })
  Assert.True(SemVer.parse("4.5.6.78") = Some { Major = 4; Minor = 5; Patch = 6; Increment = 78 })
  Assert.True(SemVer.parse("v1.2.3") = Some { SemVer.zero with Major = 1; Minor = 2; Patch = 3 })  
  Assert.True(SemVer.parse(" V4.2.7  ") = Some { SemVer.zero with Major = 4; Minor = 2; Patch = 7 })  
  Assert.True(SemVer.parse("") = None)
  Assert.True(SemVer.parse("abc") = None)

[<Fact>]
let ``SemVer.compare works correctly`` () =
  Assert.True(SemVer.compare SemVer.zero SemVer.zero = 0)
  Assert.True(SemVer.compare { SemVer.zero with Major = 1 } SemVer.zero = 1)
  Assert.True(SemVer.compare { SemVer.zero with Major = 1; Minor = 2 } { SemVer.zero with Major = 1 } = 1)

[<Fact>]
let ``Constraint.parse works correctly`` () =
  Assert.True(Constraint.parse("*") = Some Constraint.Wildcard)

[<Fact>]
let ``Constraint.satisfies works correctly`` () =
  let v = Version.RevisionVersion "aabbccddee"
  let w = Version.TagVersion "rc1"
  let c = Constraint.Exactly v
  Assert.True(Constraint.satisfies c v)
  Assert.False(Constraint.satisfies c w)

[<Fact>]
let ``ResolvedVersion.isCompatible works correctly`` () =
  let v = Version.BranchVersion "master"
  let w = Version.TagVersion "rc1"
  let r = "aabbccddee"
  let s = "llmmnnoopp"
  Assert.True(ResolvedVersion.isCompatible { Version = v; Revision = r } { Version = v; Revision = r })
  Assert.True(ResolvedVersion.isCompatible { Version = v; Revision = r } { Version = w; Revision = r })
  Assert.True(ResolvedVersion.isCompatible { Version = v; Revision = r } { Version = v; Revision = s })
  Assert.False(ResolvedVersion.isCompatible { Version = v; Revision = r } { Version = w; Revision = s })

[<Fact>]
let ``Project.parse works correctly`` () = 
  let cases = [
    ("github.com/abc/def", Project.GitHub { Owner = "abc"; Project = "def" } |> Some);
    ("github+abc/def", Project.GitHub { Owner = "abc"; Project = "def" } |> Some);
    ("", None)
  ]
  for (input, expected) in cases do
    Assert.True((Project.parse input = expected))

// [<Fact>]
// let ``Manifest.parse works correctly`` () =
//   let content = "{ \"dependencies\": { \"github+njlr/test-lib-b\": \"*\" } }"
//   let manifest = 
//     { 
//       Dependencies = 
//         [ 
//           { 
//             Project = Project.GitHub { Owner = "njlr"; Project = "test-lib-b" }; 
//             Constraint = Constraint.Wildcard 
//           } 
//         ] 
//     }
//   Assert.True(Manifest.parse content = Some manifest)
