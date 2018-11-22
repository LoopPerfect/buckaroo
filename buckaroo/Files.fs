module Buckaroo.Files

open System.IO

let exists (path : string) = async {
  return File.Exists(path)
}

let mkdirp (path : string) = async {
  if Directory.Exists(path) |> not
  then
    Directory.CreateDirectory(path) |> ignore
  return ()
}

let writeFile (path : string) (content : string) = async {
  use sw = new System.IO.StreamWriter(path)
  return! sw.WriteAsync(content) |> Async.AwaitTask
}

let readFile (path : string) = async {
  use sr = new StreamReader(path)
  return! sr.ReadToEndAsync() |> Async.AwaitTask
}

let deleteDirectoryIfExists (path : string) = 
  async {
    try 
      Directory.Delete(path, true) 
      return true
    with
    | ex -> 
      return
        if ex :? DirectoryNotFoundException 
        then false
        else raise ex
  }
  |> Async.StartAsTask
  |> Async.AwaitTask

let rec copyDirectory srcPath dstPath = 
  async {

    if not <| System.IO.Directory.Exists(srcPath) then
      let msg = System.String.Format("Source directory does not exist or could not be found: {0}", srcPath)
      raise (System.IO.DirectoryNotFoundException(msg))

    if not <| System.IO.Directory.Exists(dstPath) then
      System.IO.Directory.CreateDirectory(dstPath) |> ignore

    let srcDir = new System.IO.DirectoryInfo(srcPath)

    for file in srcDir.GetFiles() do
      let temppath = System.IO.Path.Combine(dstPath, file.Name)
      file.CopyTo(temppath, true) |> ignore

    for subdir in srcDir.GetDirectories() do
      let dstSubDir = System.IO.Path.Combine(dstPath, subdir.Name)
      do! copyDirectory subdir.FullName dstSubDir 

    ()
  } 
  |> Async.StartAsTask
  |> Async.AwaitTask
