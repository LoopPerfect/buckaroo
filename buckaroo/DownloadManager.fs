namespace Buckaroo

open System
open System.IO
open System.Text.RegularExpressions
open FSharp.Data
open FSharpx.Control
open Buckaroo.Hashing

type DownloadManager (cacheDirectory : string) = 
  
  let mutable cache = Map.empty

  let sanitizeFilename (x : string) = 
    let regexSearch = 
      new string(Path.GetInvalidFileNameChars()) + 
      new string(Path.GetInvalidPathChars()) + 
      "@.:\\/";
    let r = new Regex(String.Format("[{0}]", Regex.Escape(regexSearch)))
    Regex.Replace(r.Replace(x, "-"), "-{2,}", "-")

  let cachePath (url : string) = 
    let hash = sha256 url
    Path.Combine(cacheDirectory, (sanitizeFilename url).ToLower() + "-" + hash.Substring(0, 16))

  let downloadFile (url : string) (target : string) = async {
    System.Console.WriteLine ("Downloading " + url + " to " + target + "... ")
    let! request = Http.AsyncRequestStream url
    use outputFile = new FileStream(target, FileMode.Create)
    do! 
      request.ResponseStream.CopyToAsync outputFile 
      |> Async.AwaitTask
  }

  member this.DonwloadToCache (url : string) = 
    match cache |> Map.tryFind url with 
    | Some task -> task
    | None -> 
      let task = 
        async {
          let target = cachePath url
          if File.Exists target |> not
          then
            do! Files.mkdirp (Path.GetDirectoryName target)
            do! downloadFile url target
          return target
        }
        |> Async.Cache
      cache <- cache |> Map.add url task
      task

  member this.Download (url : string) (path : string) = async {
    let! source = this.DonwloadToCache url
    do! Files.copy source path
  }

  member this.DownloadHash (hash : string) (urls : string list) = async {
    
  }
