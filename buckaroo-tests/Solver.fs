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

let branch b = Version.Git (GitVersion.Branch b)
let rev (x : int) = Version.Git(GitVersion.Revision (x.ToString()))
let ver (x : int) = Version.SemVer {SemVer.zero with Major = x}

type TestingSourceExplorer (manifestSpec : List<PackageIdentifier * Version * Manifest>) =

  interface ISourceExplorer with
    member this.FetchVersions (_ : PackageSources) (package: PackageIdentifier) : AsyncSeq<Version>  = asyncSeq {

      yield!
        manifestSpec
          |> Seq.choose (fun (p, v, _) ->
            if p = package
            then Some v
            else None)
          |> AsyncSeq.ofSeq
    }

    member this.FetchLocations (_: PackageSources) (package: PackageIdentifier) (version: Version) : AsyncSeq<PackageLocation> = asyncSeq {
        match package, version with
        | (Adhoc a, SemVer v) ->
          yield PackageLocation.GitHub {
            Package = { Owner = a.Owner; Project = a.Project };
            Revision = v.Major.ToString()
          }
        | (Adhoc a, _) ->
          yield! manifestSpec
            |> List.filter (fun (p, _, _) -> p = package )
            |> List.map(fun (_, v,_) -> v)
            |> List.choose(fun x ->
                match x with
                | SemVer v -> Some v
                | _ -> None)
            |> List.map(fun v ->
                PackageLocation.GitHub {
                  Package = { Owner = a.Owner; Project = a.Project };
                  Revision = v.Major.ToString()
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
          |> List.filter (fun (p, v, _) -> p = package g.Package.Project && v = (g.Revision.ToString() |> int |> ver))
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


let dep (p : string, vs : List<Version>) : Buckaroo.Dependency = {
    Package = package p;
    Constraint = Any ( vs |> List.map (Exactly));
    Targets = None
}

let manifest xs = {
  Manifest.zero
    with Dependencies = xs |> List.map dep |> Set.ofList
}

type ManifestSpec = List<PackageIdentifier * Version * Manifest>

let solve (manifests : ManifestSpec) style root =
    let console = new ConsoleManager(LoggingLevel.Debug);
    let context : TaskContext = {
      Console = console;
      DownloadManager = DownloadManager(console, "/tmp");
      GitManager = new GitManager(new GitCli(console), "/tmp");
      SourceExplorer = TestingSourceExplorer(manifests)
    }

    Buckaroo.Solver.solve context root style None

let inSolution (r: Resolution) (p : string, v : int) =
  match r with
  | Ok solution ->
    let (resolved, _) = solution.Resolutions.[package p]
    System.Console.WriteLine resolved
    resolved.Versions |> Set.contains (rev v)
  | _ -> false
()

[<Fact>]
let ``Solver handles simple case`` () =
  let spec = [
    (package "a", ver 2, manifest [("b", [ver 1])])
    (package "a", ver 1, manifest [("b", [ver 1])])
    (package "b", ver 1, manifest [])
  ]

  let root = manifest [("a", [branch ""])]
  let r = solve spec ResolutionStyle.Quick root
          |> Async.RunSynchronously

  Assert.True (("a", 2) |> inSolution r)
  Assert.True (("b", 1) |> inSolution r)

  ()