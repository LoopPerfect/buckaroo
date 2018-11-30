namespace Buckaroo

open System
open System.IO
open System.Text.RegularExpressions
open FSharp.Data
open FSharpx.Control
open Buckaroo.Hashing

type DownloadMessage = 
| Download of string * AsyncReplyChannel<Async<string>>

type DownloadManager (cacheDirectory : string) = 
  
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
    return target
  }

  let downloadCache = MailboxProcessor.Start(fun inbox -> async {
    let cache = new System.Collections.Generic.Dictionary<_, _>()

    while true do
      let! (Download(url, repl)) = inbox.Receive()
      if cache.ContainsKey url |> not then 
        let target = cachePath url
        let! proc = 
          downloadFile url target 
          |> Async.StartChild
        cache.Add(url, proc)
      repl.Reply(cache.[url]) 
  })

  member this.DonwloadToCache (url : string) = 
    async {
      let! res = downloadCache.PostAndAsyncReply(fun ch -> Download(url, ch))
      return! res 
    }

  member this.Download (url : string) (path : string) = async {
    let! source = this.DonwloadToCache url
    do! Files.copy source path
  }
