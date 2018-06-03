open System
open System.IO

let readFile path = async {
  return IO.File.ReadAllText "buckaroo.txt"
}

[<EntryPoint>]
let main argv =
  // let p = Project.Project.GitHub { Owner = "njlr"; Project = "test-lib-a" }
  // let v = Version.SemVerVersion { SemVer.zero with Minor = 2 }
  // let v = Version.Tag "v0.2.0"
  // let v = Version.Branch "master"
  // let r = "0aea4adf4a3b8c11eaaa14e87fa3552d7680d02a"
  // let v = Version.Revision r
  // let a : Atom.Atom = { Project = p; Version = v }
  // let versions = 
  //   SourceManager.fetchVersions p 
  //     |> Async.RunSynchronously
  //     |> Seq.map Version.show
  //     |> String.concat ", "
  // Console.WriteLine versions
  // let revisions = 
  //   SourceManager.fetchRevisions a
  //     |> Async.RunSynchronously
  //     |> String.concat ", "
  // Console.WriteLine revisions
  // let manifest = 
  //   SourceManager.fetchManifest p r 
  //   |> Async.RunSynchronously
  // manifest.Dependencies 
  //   |> Seq.map Dependency.show 
  //   |> String.concat "\n" 
  //   |> Console.WriteLine
  let content = "buckaroo.txt" |> readFile |> Async.RunSynchronously
  let manifest = Manifest.parse content |> Option.get
  Console.WriteLine "Manifest: "
  Console.WriteLine (Manifest.show manifest)
  Console.WriteLine "Resolving... "
  let resolution = Solver.solve manifest |> Async.RunSynchronously
  Console.WriteLine (Solver.show resolution)
  0
