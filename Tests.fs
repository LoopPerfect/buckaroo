module Tests

open System
open Xunit

open Target
open Constraint
open Dependency
open Manifest

let dropError<'T, 'E> (x : Result<'T, 'E>) =
  match x with 
  | Result.Ok o -> Some o
  | Result.Error _ -> None

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
  ]
  for (input, expected) in cases do
    Assert.Equal(expected, SemVer.parse input |> dropError)

[<Fact>]
let ``SemVer.compare works correctly`` () =
  Assert.True(SemVer.compare SemVer.zero SemVer.zero = 0)
  Assert.True(SemVer.compare { SemVer.zero with Major = 1 } SemVer.zero = 1)
  Assert.True(SemVer.compare { SemVer.zero with Major = 1; Minor = 2 } { SemVer.zero with Major = 1 } = 1)

[<Fact>]
let ``Version.parse works correctly`` () =
  let cases = [
    ("tag=abc", Version.Tag "abc" |> Some);
    ("tag=foo/bar", Version.Tag "foo/bar" |> Some);
    ("tag=v3.0.0", Version.Tag "v3.0.0" |> Some);
    ("branch=master", Version.Branch "master" |> Some);
    ("revision=aabbccddee", Version.Revision "aabbccddee" |> Some);
    ("1.2", Version.SemVerVersion { SemVer.zero with Major = 1; Minor = 2 } |> Some);
    ("", None)
  ]
  for (input, expected) in cases do
    Assert.Equal(expected, Version.parse input)
 
[<Fact>]
let ``Version.harmonious works correctly`` () =
  let cases = [
    (Version.Revision "aabbccddee", Version.Tag "aabbccddee", true);
    (Version.Tag "abc", Version.Tag "abc", true);
    (Version.Branch "master", Version.Branch "master", true);
    (Version.SemVerVersion SemVer.zero, Version.SemVerVersion SemVer.zero, true);
    (Version.SemVerVersion SemVer.zero, Version.Revision "aabbccddee", true);
    (Version.SemVerVersion SemVer.zero, Version.Tag "abc", true);
    (Version.Branch "master", Version.Branch "develop", false);
  ]
  for (v, u, expected) in cases do
    Assert.Equal(expected, Version.harmonious v u)

[<Fact>]
let ``Constraint.parse works correctly`` () =
  let cases = [
    ("*", Constraint.wildcard |> Some); 
    ("revision=aabbccddee", "aabbccddee" |> Version.Revision |> Exactly |> Some); 
    ("!*", Constraint.wildcard |> Constraint.Complement |> Some); 
    ("any(branch=master)", Some(Any [Exactly (Version.Branch "master")])); 
    ("any(revision=aabbccddee branch=master)", Some(Any [Exactly (Version.Revision "aabbccddee"); Exactly (Version.Branch "master")])); 
    ("all(*)", Some(All [Constraint.wildcard])); 
    ("", None); 
  ]
  for (input, expected) in cases do
    Assert.Equal(expected, Constraint.parse input |> dropError)

[<Fact>]
let ``Constraint.satisfies works correctly`` () =
  let v = Version.Revision "aabbccddee"
  let w = Version.Tag "rc1"
  let c = Constraint.Exactly v
  Assert.True(Constraint.satisfies c v)
  Assert.False(Constraint.satisfies c w)

[<Fact>]
let ``Constraint.agreesWith works correctly`` () =
  let v = Version.Revision "aabbccddee"
  let w = Version.Tag "rc1"
  let x = Version.Revision "ffgghhiijjkk"
  let c = Constraint.Exactly v
  Assert.True(Constraint.agreesWith c v)
  Assert.True(Constraint.agreesWith c w)
  Assert.False(Constraint.agreesWith c x)

[<Fact>]
let ``Target.parse works correctly`` () = 
  let input = "//path/to/some:target"
  let expected = Result.Ok { Folders = [ "path"; "to"; "some" ]; Name = "target" }
  Assert.Equal(expected, Target.parse input)
  
  let input = "//:foo"
  let expected = Result.Ok { Folders = []; Name = "foo" }
  Assert.Equal(expected, Target.parse input)
  
  let input = "//foo"
  let expected = Result.Ok { Folders = [ "foo" ]; Name = "foo" }
  Assert.Equal(expected, Target.parse input)
  
  let input = "//foo/bar"
  let expected = Result.Ok { Folders = [ "foo"; "bar" ]; Name = "bar" }
  Assert.Equal(expected, Target.parse input)
  
  let input = "//abc/def"
  let expected = Result.Ok { Folders = [ "abc"; "def" ]; Name = "def" }
  Assert.Equal(expected, Target.parse input)

  let input = ":bar"
  let expected = Result.Ok { Folders = []; Name = "bar" }
  Assert.Equal(expected, Target.parse input)

[<Fact>]
let ``Dependency.parse works correctly`` () =
  let p = Project.GitHub { Owner = "abc"; Project = "def" }
  let cases = [
    ("github.com/abc/def@*", Some({ Project = p; Constraint = Constraint.wildcard; Target = None }))
    ("github.com/abc/def@*//:foo", Some({ Project = p; Constraint = Constraint.wildcard; Target = Some { Folders = []; Name = "foo" } }))
    ("", None); 
  ]
  for (input, expected) in cases do
    Assert.Equal(expected, Dependency.parse input)

[<Fact>]
let ``ResolvedVersion.isCompatible works correctly`` () =
  let v = Version.Branch "master"
  let w = Version.Tag "rc1"
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
    ("github.com/abc/def.ghi", Project.GitHub { Owner = "abc"; Project = "def.ghi" } |> Some);
    ("github+abc/def", Project.GitHub { Owner = "abc"; Project = "def" } |> Some);
    ("", None)
  ]
  for (input, expected) in cases do
    Assert.Equal(expected, Project.parse input |> dropError)

[<Fact>]
let ``Manifest.parse works correctly`` () =
  let a = { 
    Project = Project.GitHub { Owner = "abc"; Project = "def" }; 
    Constraint = Constraint.wildcard; 
    Target = None
  } 
  let b = { 
    Project = Project.GitHub { Owner = "ijk"; Project = "xyz" }; 
    Constraint = Constraint.wildcard; 
    Target = Some { Folders = []; Name = "foo" }
  } 
  let cases = [
    ("", Ok { Manifest.zero with Dependencies = set [] });
    ("[[dependency]]\nname = \"github.com/abc/def\"\nversion = \"*\"", Ok { Manifest.zero with Dependencies = set [ a ] });
    ("[[dependency]]\nname = \"github.com/ijk/xyz\"\nversion = \"*\"\ntarget = \"//:foo\"", Ok { Manifest.zero with Dependencies = set [ b ] });
  ]
  for (input, expected) in cases do
    Assert.Equal(expected, Manifest.parse input)
