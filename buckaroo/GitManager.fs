namespace Buckaroo.Git

open System
open System.IO
open System.Security.Cryptography
open System.Text.RegularExpressions
open FSharpx.Control
open Buckaroo

type CloneRequest = 
  | CloneRequest of string * AsyncReplyChannel<Async<string>>

type GitManager (git : IGit, cacheDirectory : string) = 

  let mutable tagsCache = Map.empty
  let mutable headsCache = Map.empty

  let bytesToHex bytes = 
    bytes 
    |> Array.map (fun (x : byte) -> System.String.Format("{0:x2}", x))
    |> String.concat System.String.Empty

  let sanitizeFilename (x : string) = 
    let regexSearch = 
      new string(Path.GetInvalidFileNameChars()) + 
      new string(Path.GetInvalidPathChars()) + 
      "@.:\\/";
    let r = new Regex(String.Format("[{0}]", Regex.Escape(regexSearch)))
    Regex.Replace(r.Replace(x, "-"), "-{2,}", "-")

  let cloneFolderName (url : string) = 
    let bytes = System.Text.Encoding.UTF8.GetBytes url
    let hash = 
      bytes 
      |> (new SHA256Managed()).ComputeHash 
      |> bytesToHex
    (sanitizeFilename(url)).ToLower() + "-" + hash.Substring(0, 16)

  let mailboxProcessor = MailboxProcessor.Start(fun inbox -> async {
    let mutable cloneCache : Map<string, Async<string>> = Map.empty
    while true do
      let! message = inbox.Receive()
      let (CloneRequest(url, replyChannel)) = message
      match cloneCache |> Map.tryFind url with
      | Some task -> 
        replyChannel.Reply(task)
      | None -> 
        let targetDirectory = Path.Combine(cacheDirectory, cloneFolderName url)
        let task = 
          async {
            if Directory.Exists targetDirectory |> not
            then 
              do! git.Clone url targetDirectory
            return targetDirectory
          }
          |> Async.Cache
        cloneCache <- cloneCache |> Map.add url task
        replyChannel.Reply(task) 
  })

  member this.Clone (url : string) : Async<string> = async {
    let! res = mailboxProcessor.PostAndAsyncReply(fun ch -> CloneRequest(url, ch))
    return! res 
  }

  member this.Fetch (url : string) (branch : string) : Async<Unit> = async {
    let! targetDirectory = this.Clone(url)
    return! 
      git.FetchBranch targetDirectory branch
      |> Async.Ignore
  }
  member this.FetchTags (url : string) = async {
    match tagsCache |> Map.tryFind url with
    | Some tags -> return tags
    | None -> 
      let! tags = git.RemoteTags url
      tagsCache <- tagsCache |> Map.add url tags
      return tags
  }

  member this.FetchBranches (url : string) = async {
    match headsCache |> Map.tryFind url with
    | Some heads -> return heads
    | None -> 
      let! heads = git.RemoteHeads url
      headsCache <- headsCache |> Map.add url heads
      return heads
  }

  member this.FetchFile (url : string) (revision : Revision) (file : string) : Async<string> = 
    async {
      let! targetDirectory = this.Clone(url)
      try
        return! git.FetchFile targetDirectory revision file
      with _ -> 
        do! git.FetchCommit targetDirectory revision
        return! git.FetchFile targetDirectory revision file
    }

  member this.FetchCommits (url : string) (branch : Branch) = async {
    let! targetDirectory = this.Clone(url)
    do! 
      git.FetchBranch targetDirectory branch
      |> Async.Ignore
    return! git.FetchCommits targetDirectory branch
  }
