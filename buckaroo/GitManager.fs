namespace Buckaroo

open System
open System.IO
open System.Security.Cryptography
open System.Text.RegularExpressions
open FSharpx.Control


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

  member private this.getBranchHint (targetDirectory : string) (hint: Option<Branch>) = async {
    return!
      match hint with
      | None -> this.DefaultBranch targetDirectory
      | Some b -> async { return b }
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

  member this.FetchCommit (url : string) (commit : string) (hint : Option<Branch>) : Async<Unit> = async {
    let! targetDirectory = this.Clone(url)
    let operations = [
      fun () -> async { return () };
      fun () -> async {
        let! branchHint = this.getBranchHint targetDirectory hint
        do! git.FetchBranch targetDirectory branchHint
      };
      fun () -> git.Unshallow targetDirectory;
    ]

    let! success =
      let rec loop =
        function
          | [] -> async { return false }
          | op::ops -> async {
            do! op()
            let! success = git.HasCommit targetDirectory commit
            return!
              match success with
              | true -> async { return true }
              | false -> loop ops
          }
      loop operations
    if not success then
      raise <| new Exception("failed to find fetch: " + url + " " + commit)
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
      let! refs = git.RemoteRefs url
      refsCache <- refsCache |> Map.add url refs
      return refs
  }
  member this.FetchFile (url : string) (revision : Revision) (file : string) (hint : Option<Branch>) : Async<string> =
    async {
      let! targetDirectory = this.Clone(url)
      do! this.FetchCommit url revision hint
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
