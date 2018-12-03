module Buckaroo.Archive

open System
open System.IO
open SharpCompress
open SharpCompress.Common
open SharpCompress.Archives

let private extractRoot (archive : IArchive) (pattern : string) = 
  let candidates = 
    archive.Entries 
    |> Seq.filter (fun e -> e.IsDirectory)
    |> Seq.map (fun e -> e.Key)
    |> Seq.filter (Glob.isLike pattern)
    |> Seq.distinct
    |> Seq.truncate 2
    |> Seq.toList
  match candidates with 
  | head::[] -> head
  | [] -> 
    raise <| new Exception("No directories matched the root")
  | xs -> 
    raise <| new Exception("Multiple directories match the root: " + (string xs))

let extractTo (pathToArchive : string) (pathToExtraction : string) (stripPrefix : string option) = async {
  use archive = Archives.Zip.ZipArchive.Open(pathToArchive) :> IArchive
  
  let extractionOptions = new ExtractionOptions()
  extractionOptions.ExtractFullPath <- false
  extractionOptions.Overwrite <- true

  let root =
    stripPrefix
    |> Option.map (extractRoot archive)
    |> Option.defaultValue ""

  let directoriesToExtract = 
    archive.Entries 
    |> Seq.filter (fun e -> e.IsDirectory)
    |> Seq.map (fun e -> e.Key)
    |> Seq.filter (fun e -> e.StartsWith root)
    |> Seq.distinct

  let entriesToExtract = 
    archive.Entries 
    |> Seq.filter (fun e -> not e.IsDirectory)
    |> Seq.filter (fun e -> e.Key.StartsWith root)
  
  for directory in directoriesToExtract do
    let target = Path.Combine(pathToExtraction, directory.Substring(root.Length))
    do! Files.mkdirp target

  for entry in entriesToExtract do
    let subPath = entry.Key.Substring(root.Length)
    let target = Path.Combine(pathToExtraction, subPath)
    do entry.WriteToFile(target, extractionOptions)
}
