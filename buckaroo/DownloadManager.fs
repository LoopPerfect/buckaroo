namespace Buckaroo

open System
open System.IO
open System.Text.RegularExpressions
open FSharp.Data
open FSharpx.Control
open Buckaroo.RichOutput
open Buckaroo.Console
open Buckaroo.Hashing

type CopyMessage = 
| Copy of string * string * AsyncReplyChannel<Async<Unit>>

type DownloadMessage = 
| Download of string * AsyncReplyChannel<Async<string>>

type DownloadManager (console : ConsoleManager, cacheDirectory : string) = 
  
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

  let cachePathHash (hash : string) = 
    Path.Combine(cacheDirectory, hash)

  let downloadFile (url : string) (target : string) = async {
    console.Write (
      (text "Downloading ") + 
      (text url |> foreground ConsoleColor.Magenta) + 
      " to " + 
      (text target |> foreground ConsoleColor.Cyan) + "... ")
    let! request = Http.AsyncRequestStream url
    use outputFile = new FileStream(target, FileMode.Create)
    do! 
      request.ResponseStream.CopyToAsync outputFile 
      |> Async.AwaitTask
    return target
  }

  let hashCache = MailboxProcessor.Start (fun inbox -> async {
    let mutable cache = Map.empty

    while true do
      let! (Copy(source, destination, replyChannel)) = inbox.Receive()
      match cache |> Map.tryFind destination with
      | Some task -> replyChannel.Reply(task)
      | None -> 
        let! task = 
          async {
            if File.Exists destination |> not
            then
              do! Files.copy source destination
          }
          |> Async.StartChild
        cache <- cache |> Map.add destination task
        replyChannel.Reply(task)
  })

  let copy source destination = async {
    let! task = hashCache.PostAndAsyncReply (fun ch -> Copy (source, destination, ch))
    
    return! task
  }

  let downloadCache = MailboxProcessor.Start (fun inbox -> async {
    let mutable cache = Map.empty

    while true do
      let! (Download (url, replyChannel)) = inbox.Receive ()
      
      match cache |> Map.tryFind url with
      | Some task -> replyChannel.Reply(task)
      | None -> 
        let target = cachePath url
        let! task = 
          async {
            if File.Exists target
            then
              console.Write ((text "Deleting ") + (text target |> foreground ConsoleColor.Cyan) + "... ")
              do! Files.delete target
            let! cachePath = downloadFile url target 
            let! hash = Files.sha256 cachePath
            let destination = cachePathHash hash
            do! copy cachePath destination
            return destination
          }
          |> Async.StartChild
          
        cache <- cache |> Map.add url task
        replyChannel.Reply(task)
  })

  member this.DownloadToCache (url : string) = async {
    let! res = downloadCache.PostAndAsyncReply(fun ch -> Download(url, ch))
    return! res 
  }

  member this.Download (url : string) (path : string) = async {
    let! source = this.DownloadToCache url
    do! Files.copy source path
  }

  member this.DownloadHash (sha256 : string) (urls : string list) : Async<string> = 
    let rec processUrls urls = async {
      match urls with
      | head::tail -> 
        let! cachePath = this.DownloadToCache head
        let! actualHash = Files.sha256 cachePath
        if actualHash = sha256
        then 
          return cachePath
        else
          return! processUrls tail
      | [] -> 
        return raise <| new Exception("Ran out of URLs to try")
    }
    processUrls urls
