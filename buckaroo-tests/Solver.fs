module Buckaroo.Tests.Solver

open Xunit
open Buckaroo
open FSharp.Control

open Buckaroo.Console
open Buckaroo.Tasks

let package name = PackageIdentifier.Adhoc {
  Owner = "test";
  Project = name
}

let br b = Version.Git (GitVersion.Branch b)
let rev (x : int) = Version.Git(GitVersion.Revision (x.ToString()))
let ver (x : int) = Version.SemVer {SemVer.zero with Major = x}

type TestingSourceExplorer (manifestSpec : List<PackageIdentifier * Set<Version> * Manifest>) =

  interface ISourceExplorer with
    member this.FetchVersions (_ : PackageSources) (package: PackageIdentifier) : AsyncSeq<Version>  = asyncSeq {
      yield!
        manifestSpec
          |> Seq.choose (fun (p, v, _) ->
            if p = package
            then Some (v |> Set.toSeq |> Seq.sortDescending)
            else None)
          |> Seq.collect (id)
          |> AsyncSeq.ofSeq
    }

    member this.FetchLocations (_: PackageSources) (package: PackageIdentifier) (version: Version) : AsyncSeq<PackageLocation> = asyncSeq {
        match package with
        | (Adhoc a) ->
          yield! manifestSpec
            |> Seq.filter (fun (p, vs, _) -> p = package && vs |> Set.contains version)
            |> Seq.map(fun (_, vs, _) -> vs)
            |> Seq.collect(Set.toList)
            |> Seq.choose(fun x ->
                match x with
                | Version.Git (GitVersion.Revision r) -> Some r
                | SemVer v -> Some (v.Major.ToString())
                | _ -> None)
            |> Seq.map(fun r ->
                PackageLocation.GitHub {
                  Package = { Owner = a.Owner; Project = a.Project };
                  Revision = r
                })
            |> AsyncSeq.ofSeq
        | _ -> raise <| new System.SystemException "package not found"
    }

    member this.LockLocation (location : PackageLocation) : Async<PackageLock> = async {
      let x =
        match location with
        | PackageLocation.GitHub g -> Some (PackageLock.GitHub g)
        | _ -> None

      return x |> Option.get
    }

    member this.FetchManifest (lock : PackageLock) : Async<Manifest> = async {
      let x =
        match lock with
        | PackageLock.GitHub g ->
          manifestSpec
          |> List.filter (fun (p, v, _) -> p = package g.Package.Project && v.Contains(g.Revision.ToString() |> int |> ver))
          |> List.map (fun (_, _, m) -> m)
          |> List.tryHead
        | _ -> None

      return x |> Option.get
    }

    member this.FetchLock (lock : PackageLock) : Async<Lock> = async {
      raise <| new System.SystemException "Ignore Me"
      return {
        ManifestHash = "";
        Dependencies = Set.empty
        Packages = Map.empty
      }
    }


let dep (p : string, c: Constraint) : Buckaroo.Dependency = {
    Package = package p;
    Constraint = c;
    Targets = None
}

let manifest xs = {
  Manifest.zero
    with Dependencies = xs |> List.map dep |> Set.ofList
}

type ManifestSpec = List<PackageIdentifier * Set<Version> * Manifest>

let solve (manifests : ManifestSpec) style root =
    let console = new ConsoleManager(LoggingLevel.Debug);
    let context : TaskContext = {
      Console = console;
      DownloadManager = DownloadManager(console, "/tmp");
      GitManager = new GitManager(new GitCli(console), "/tmp");
      SourceExplorer = TestingSourceExplorer(manifests)
    }

    Buckaroo.Solver.solve context root style None

let getLockedRev (p : string) (r: Resolution) =
  match r with
  | Ok solution ->
    let (resolved, _) = solution.Resolutions.[package p]
    match resolved.Lock with
    | PackageLock.GitHub g -> g.Revision
    | _ -> ""
  | _ -> ""
()

let isOk (r: Resolution) =
  match r with
  | Ok _ -> true
  | _ -> false

[<Fact>]
let ``Solver handles simple case`` () =
  let spec = [
    (package "a",
      Set[ver 2; br "a"],
      manifest [("b", Exactly (ver 1) )])
    (package "a",
      Set[ver 1; br "a"],
      manifest [("b", Exactly (ver 1) )])
    (package "b",
      Set[ver 1; br "a"],
      manifest [])
  ]

  let root = manifest [("a", Exactly (br "a") )]
  let solution =
    solve spec ResolutionStyle.Quick root
      |> Async.RunSynchronously

  Assert.Equal ("2", getLockedRev "a" solution)
  Assert.Equal ("1", getLockedRev "b" solution)
  ()

[<Fact>]
let ``Solver can backtrack to resolve simple conflicts`` () =
  let spec = [
    (package "a",
      Set[ver 2; br "a"],
      manifest [("b", Exactly (ver 2) )])
    (package "a",
      Set[ver 1; br "a"],
      manifest [("b", Exactly (ver 1) )])
    (package "b",
      Set[ver 1; br "a"],
      manifest [])
  ]

  let root = manifest [("a", Exactly (br "a") )]
  let solution =
    solve spec ResolutionStyle.Quick root
      |> Async.RunSynchronously

  Assert.Equal ("1", getLockedRev "a" solution)
  Assert.Equal ("1", getLockedRev "b" solution)
  ()


[<Fact>]
let ``Solver can compute version intersections`` () =

  let root = manifest [
    ("a", Exactly (ver 1) )
    ("b", Exactly (ver 1) )
  ]
  let spec = [
    (package "a",
      Set[ver 1; br "a"],
      manifest [("b", Exactly (br "a") )])
    (package "b",
      Set[ver 1; br "a"],
      manifest [])
  ]

  let solution =
    solve spec ResolutionStyle.Quick root
      |> Async.RunSynchronously

  Assert.Equal ("1", getLockedRev "a" solution)
  Assert.Equal ("1", getLockedRev "b" solution)
  ()

[<Fact>]
let ``Solver can compute intersection of branches`` () =

  let root = manifest [
    ("a", All [Exactly (br "b"); Exactly (br "a")])
  ]

  let spec = [
    (package "a",
      Set[ver 1; br "a"],
      manifest [])
    (package "a",
      Set[ver 2; br "a"],
      manifest [])
    (package "a",
      Set[ver 3; br "a"; br "b"],
      manifest [])
  ]

  let solution =
    solve spec ResolutionStyle.Quick root
      |> Async.RunSynchronously

  Assert.Equal ("3", getLockedRev "a" solution)
  ()

[<Fact>]
let ``Solver fails if package cant satisfy all constraints`` () =

  let root = manifest [
    ("a", Exactly (br "a"))
    ("b", Exactly (br "b"))
  ]

  let spec = [
    (package "a",
      Set[ver 1; br "a"],
      manifest [("b", Exactly (br "a"))])
    (package "b",
      Set[ver 1; br "a"],
      manifest [])
    (package "b",
      Set[ver 2; br "b"],
      manifest [])
  ]

  let solution =
    solve spec ResolutionStyle.Quick root
      |> Async.RunSynchronously

  Assert.False (isOk solution)
  ()


[<Fact>]
let ``Solver picks package that satisfies all constraints`` () =

  let root = manifest [
    ("a", Exactly (br "a"))
    ("b", Exactly (br "b"))
  ]

  let spec = [
    (package "a",
      Set[ver 1; br "a"],
      manifest [("b", Exactly (br "a"))])
    (package "b",
      Set[ver 1; br "a"],
      manifest [])
    (package "b",
      Set[ver 2; br "b"],
      manifest [])
    (package "b",
      Set[ver 3; br "a"; br "b"],
      manifest [])
  ]

  let solution =
    solve spec ResolutionStyle.Quick root
      |> Async.RunSynchronously

  Assert.Equal ("3", getLockedRev "b" solution)
  ()
