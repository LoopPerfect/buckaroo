open System
open System.IO

[<EntryPoint>]
let main argv =
  let p = Project.Project.GitHub { Owner = "parro-it"; Project = "libui-node" }
  let versions = 
    SourceManager.fetchVersions p 
      |> Async.RunSynchronously
      |> Seq.map Version.show
      |> String.concat ", "
  Console.WriteLine versions
  0
