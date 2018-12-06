module Buckaroo.Files

open System.IO
open Buckaroo.Hashing

[<Literal>]
let private DefaultBufferSize = 4096

let exists (path : string) = async {
  return File.Exists(path)
}

let directoryExists (path : string) = async {
  return Directory.Exists(path)
}

let delete (path : string) = async {
  return File.Delete path
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

let touch (path : string) = async {
  if File.Exists path |> not 
  then 
    do! writeFile path ""
}

let readFile (path : string) = async {
  use sr = new StreamReader(path)
  return! sr.ReadToEndAsync() |> Async.AwaitTask
}

let copy (source : string) (destination : string) = async {
  use sourceFile = new FileStream(source, FileMode.Open, FileAccess.Read, FileShare.Read, DefaultBufferSize, true)
  use destFile = new FileStream(destination, FileMode.OpenOrCreate, FileAccess.Write, FileShare.None, DefaultBufferSize, true)
  do! 
    sourceFile.CopyToAsync(destFile) 
    |> Async.AwaitTask
}

let sha256 (path : string) = async {
  let! content = readFile path
  return sha256 content
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

let rec copyDirectory srcPath dstPath = async {
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
