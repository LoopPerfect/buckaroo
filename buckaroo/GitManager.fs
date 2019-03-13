namespace Buckaroo

open System
open System.IO
open System.Security.Cryptography
open System.Text.RegularExpressions
open FSharp.Control
open FSharpx
open FSharpx.Control
open Buckaroo.Console
open Buckaroo.Logger
open Buckaroo.RichOutput

type GitManagerRequest =
| CloneRequest of string * AsyncReplyChannel<Async<string>>
| FetchRefs of string * AsyncReplyChannel<Async<Ref list>>

type FetchStyle =
| RemoteFirst
| CacheFirst

type GitManager (style: FetchStyle, console : ConsoleManager, git : IGit, cacheDirectory : string) =

  let logger = createLogger console (Some "git")

  let mutable refsCache = Map.empty

  let bytesToHex bytes =
    bytes
    |> Array.map (fun (x : byte) -> System.String.Format("{0:x2}", x))
    |> String.concat System.String.Empty

  let sanitizeFilename (x : string) =
    let regexSearch =
      new string(Path.GetInvalidFileNameChars()) +
      new string(Path.GetInvalidPathChars()) +
      "@.:\\/"

    let r = Regex(String.Format("[{0}]", Regex.Escape(regexSearch)))

    Regex.Replace(r.Replace(x, "-"), "-{2,}", "-")

  let cloneFolderName (url : string) =
    let bytes = System.Text.Encoding.UTF8.GetBytes url
    let hash =
      bytes
      |> (new SHA256Managed()).ComputeHash
      |> bytesToHex
    let folder = sanitizeFilename(url).ToLower() + "-" + hash.Substring(0, 16)
    Path.Combine(cacheDirectory, folder)

  let pickRefsToFetch (style: FetchStyle) (remote : Async<List<Ref>>) (cache: Async<List<Ref>>) = async {
    match style with
    | RemoteFirst ->
      let! x = remote
      if x |> List.isEmpty then
        return x
      else return! cache
    | CacheFirst ->
      let! x = cache
      if x |> List.isEmpty then
        return x
      else return! remote
  }

  let mailboxCloneProcessor = MailboxProcessor.Start(fun inbox -> async {
    let mutable cloneCache : Map<string, Async<string>> = Map.empty
    while true do
      let! message = inbox.Receive()
      match message with
      | CloneRequest (url, replyChannel) ->
        match cloneCache |> Map.tryFind url with
        | Some task ->
          replyChannel.Reply(task)
        | None ->
          let targetDirectory = cloneFolderName url
          let task =
            async {
              if Directory.Exists targetDirectory |> not
              then
                do! git.ShallowClone url targetDirectory
              return targetDirectory
            }
            |> Async.Cache
          cloneCache <- cloneCache |> Map.add url task
          replyChannel.Reply(task)
      | FetchRefs (url, replyChannel) ->
        match refsCache |> Map.tryFind url with
        | Some task -> replyChannel.Reply (task)
        | None ->
          let task =
            async {
              logger.RichInfo ((text "Fetching refs from ") + (highlight url))

              let cacheDir = cloneFolderName url
              let startTime = System.DateTime.Now

              let! refs =
                pickRefsToFetch
                  style
                  (git.RemoteRefs url
                    |> Async.Catch
                    |> Async.map (Choice.toOption >> Option.defaultValue([])))
                  (git.RemoteRefs cacheDir
                    |> Async.Catch
                    |> Async.map (Choice.toOption >> Option.defaultValue([])))

              let endTime = System.DateTime.Now

              if refs |> List.isEmpty then
                raise <| SystemException("No internet connection and the cache is empty")

              logger.RichSuccess(
                (text "Fetched ") +
                (refs |> List.length |> string |> info) +
                (text " refs in ") +
                ((endTime - startTime).TotalSeconds.ToString("N3")))

              return refs
            }
            |> Async.Cache

          refsCache <- refsCache |> Map.add url task

          replyChannel.Reply (task)
  })

  member this.Clone (url : string) : Async<string> = async {
    let! res = mailboxCloneProcessor.PostAndAsyncReply(fun ch -> CloneRequest(url, ch))
    return! res
  }

  member this.CopyFromCache (gitUrl : string) (revision : Revision) (installPath : string) : Async<Unit> = async {
    let! hasGit =
      Files.directoryExists (Path.Combine (installPath, ".git/"))

    if hasGit
    then
      do! git.UpdateRefs installPath
      return! git.Checkout installPath revision
    else
      do! git.CheckoutTo (cloneFolderName gitUrl) revision installPath
  }

  member this.FindCommit (url : string) (commit : string) (maybeBranchHint : Option<string>) : Async<Unit> = async {
    let! targetDirectory = this.Clone(url)
    let operations = asyncSeq {
      yield async { return () };

      yield git.UpdateRefs targetDirectory

      match maybeBranchHint with
      | Some branch ->
        yield
          git.FetchCommits targetDirectory branch
          |> AsyncSeq.takeWhile ( (<>) commit )
          |> AsyncSeq.toListAsync
          |> Async.Ignore
      | None ->
        let! defaultBranch = git.DefaultBranch targetDirectory

        yield
          AsyncSeq.interleave
            (if defaultBranch <> "master"
             then this.FetchCommits targetDirectory "master"
             else AsyncSeq.ofSeq [])
            (this.FetchCommits targetDirectory defaultBranch)
          |> AsyncSeq.takeWhile ( (<>) commit )
          |> AsyncSeq.toListAsync
          |> Async.Ignore

      yield git.Unshallow targetDirectory;
    }

    let! success =
      operations
      |> AsyncSeq.mapAsync id
      |> AsyncSeq.mapAsync (fun _ -> git.HasCommit targetDirectory commit)
      |> AsyncSeq.skipWhile not
      |> AsyncSeq.take 1
      |> AsyncSeq.lastOrDefault false

    if not success
    then
      raise <| Exception("Failed to fetch: " + url + " " + commit)
  }

  member this.FetchRefs (url : string) = async {
    let! res = mailboxCloneProcessor.PostAndAsyncReply(fun ch -> FetchRefs(url, ch))
    return! res
  }

  member this.GetFile (url : string) (revision : Revision) (file : string) : Async<string> =
    async {
      let targetDirectory = cloneFolderName url
      // TODO: Preemptively clone and fetch
      return! git.ReadFile targetDirectory revision file
    }

  member this.FetchCommits (url : string) (branch : Branch) = asyncSeq {
    let! targetDirectory = this.Clone(url)
    yield! git.FetchCommits targetDirectory branch
  }

  member this.DefaultBranch (path) = async {
    return! git.DefaultBranch path
  }
