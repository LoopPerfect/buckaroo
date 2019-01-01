namespace Buckaroo

open System
open System.IO
open System.Security.Cryptography
open System.Text.RegularExpressions
open FSharpx.Control
open FSharp.Control
open FSharpx

type CloneRequest =
| CloneRequest of string * AsyncReplyChannel<Async<string>>

type GitManager (git : IGit, cacheDirectory : string) =

  let mutable refsCache = Map.empty

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
    let folder = sanitizeFilename(url).ToLower() + "-" + hash.Substring(0, 16)
    Path.Combine(cacheDirectory, folder)

  let mailboxProcessor = MailboxProcessor.Start(fun inbox -> async {
    let mutable cloneCache : Map<string, Async<string>> = Map.empty
    while true do
      let! message = inbox.Receive()
      let (CloneRequest(url, replyChannel)) = message
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
  })

  member private this.getBranchHint (targetDirectory : string)= async {
    return! this.DefaultBranch targetDirectory
  }
  member this.Clone (url : string) : Async<string> = async {
    let! res = mailboxProcessor.PostAndAsyncReply(fun ch -> CloneRequest(url, ch))
    return! res
  }

  member this.CopyFromCache  (gitUrl : string) (revision : Revision) (installPath : string) : Async<Unit> = async {
    let! hasGit = Files.directoryExists (Path.Combine (installPath, ".git/"))
    if hasGit then
      return! git.Checkout installPath revision
    else
      do! git.CheckoutTo (cloneFolderName gitUrl) revision installPath
  }

  member this.FetchCommit (url : string) (commit : string) : Async<Unit> = async {
    let! targetDirectory = this.Clone(url)
    let operations = asyncSeq {
      yield async { return () };
      let! branchHint = this.getBranchHint targetDirectory
      yield git.FetchBranch targetDirectory branchHint

      if branchHint <> "master" then
        yield
          git.FetchBranch targetDirectory "master"
          |> Async.Catch
          |> Async.Ignore

      yield git.Unshallow targetDirectory;
    }

    let! success =
      operations
      |> AsyncSeq.mapAsync(id)
      |> AsyncSeq.mapAsync(fun _ -> git.HasCommit targetDirectory commit)
      |> AsyncSeq.skipWhile(not)
      |> AsyncSeq.take(1)
      |> AsyncSeq.lastOrDefault(false)

    if not success then
      raise <| new Exception("failed to fetch: " + url + " " + commit)
  }
  member this.FetchBranch (url : string) (branch : string) : Async<Unit> = async {
    let! targetDirectory = this.Clone(url)
    return!
      git.FetchBranch targetDirectory branch
      |> Async.Ignore
  }
  member this.FetchRefs (url : string) = async {
    match refsCache |> Map.tryFind url with
    | Some refs -> return refs
    | None ->
      let cacheDir = cloneFolderName url
      let! refs =
        Async.Parallel
          (
            (git.RemoteRefs url
              |> Async.Catch
              |> Async.map(Choice.toOption >> Option.defaultValue([]))),
            (git.RemoteRefs cacheDir
              |> Async.Catch
              |> Async.map(Choice.toOption >> Option.defaultValue([])))
          )
        |> Async.map(fun (a, b) ->
          if a.Length = 0 && b.Length = 0 then
            raise <| new SystemException("no internet connection and cache empty")
          else if a.Length > 0
          then a
          else b
        )
      refsCache <- refsCache |> Map.add url refs
      return refs
  }
  member this.FetchFile (url : string) (revision : Revision) (file : string) : Async<string> =
    async {
      let! targetDirectory = this.Clone(url)
      do! this.FetchCommit url revision
      return! git.FetchFile targetDirectory revision file
    }

  member this.FetchCommits (url : string) (branch : Branch) = async {
    let! targetDirectory = this.Clone(url)
    do!
      git.FetchBranch targetDirectory branch
      |> Async.Ignore
    return! git.FetchCommits targetDirectory branch
  }

  member this.DefaultBranch (path) = async {
    return! git.DefaultBranch path
  }
