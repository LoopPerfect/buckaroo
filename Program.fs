open System
open System.IO

[<EntryPoint>]
let main argv =
  let p = Project.Project.GitHub { Owner = "parro-it"; Project = "libui-node" }
  let v = Version.SemVerVersion { SemVer.zero with Minor = 2 }
  // let v = Version.TagVersion "v0.2.0"
  // let v = Version.BranchVersion "master"
  let r = "46291ff331784b5831696cfe3fa02dc41117adc2"
  let v = Version.RevisionVersion r
  let a : Atom.Atom = { Project = p; Version = v }
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
  SourceManager.fetchManifest p r 
    |> Async.RunSynchronously
    |> Console.WriteLine
  0
