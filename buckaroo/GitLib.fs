namespace Buckaroo

open System
open System.IO
open LibGit2Sharp
open LibGit2Sharp.Handlers
open FSharpx
open Buckaroo.Console
open FSharp.Control

type GitLib (console : ConsoleManager) =

  let rec requestUsername url = async {
    console.Write ("Please enter a username for " + url)
    let! username = console.Read()
    let trimmed = username.Trim()
    if trimmed |> String.length > 0
    then
      console.Write trimmed
      return trimmed
    else
      return! requestUsername url
  }

  let rec requestPassword username url = async {
    console.Write ("Please enter a password for " + username + "@" + url)
    let! password = console.ReadSecret()
    let trimmed = password.Trim()
    if trimmed |> String.length > 0
    then
      console.Write trimmed
      return trimmed
    else
      return! requestPassword username url
  }

  let credentialsHandler = new CredentialsHandler(fun url username _ ->
    (
      async {
        let! selectedUsername =
          if username <> null
          then async { return username }
          else requestUsername url

        let! password = requestPassword selectedUsername url

        let credentials = new UsernamePasswordCredentials()

        credentials.Username <- selectedUsername
        credentials.Password <- password

        return credentials :> Credentials
      }
      |> Async.RunSynchronously
    )
  )

  let createSharedGitConfig (path : string) =
    "[core]\n" +
    "  repositoryformatversion = 0\n" +
    "  filemode = true\n" +
    "  bare = false\n" +
    "  logallrefupdates = true\n" +
    "[remote \"origin\"]\n" +
    "  url =" + path + "\n"+
    "  fetch = +refs/heads/*:refs/remotes/origin/*\n"

  let sharedGitClone (src : string) (destination : string) = async {
    let gitPath = Path.Combine (destination, ".git")
    do! Files.mkdirp gitPath
    do! Files.mkdirp (Path.Combine (gitPath, "info"))
    do! Files.mkdirp (Path.Combine (gitPath, "hooks"))
    do! Files.mkdirp (Path.Combine (gitPath, "refs", "heads"))
    do! Files.mkdirp (Path.Combine (gitPath, "refs", "tags"))
    do! Files.mkdirp (Path.Combine (gitPath, "objects", "info"))
    do! Files.mkdirp (Path.Combine (gitPath, "objects", "pack"))
    do! Files.writeFile (Path.Combine (gitPath, "description")) ""
    do! Files.writeFile (Path.Combine (gitPath, "info", "exclude")) ""
    do!
      Files.copyFile
        (Path.Combine (src, "HEAD"))
        (Path.Combine (gitPath, "HEAD"))

    do!
      Files.writeFile
        (Path.Combine (gitPath, "config"))
        (createSharedGitConfig src)

    do!
      Files.writeFile
        (Path.Combine (gitPath, "objects", "info", "alternatives"))
        src

    return ()
  }

  let isTag (ref: LibGit2Sharp.Reference) =
    ref.CanonicalName.Contains("refs/tags/")

  let isHead (ref: LibGit2Sharp.Reference) =
    ref.CanonicalName.Contains("refs/heads/")

  member this.Init (directory : string) = async {
    Repository.Init (directory, true) |> ignore
  }

  member this.LocalTags (repository : string) = async {
    let repo = new Repository(repository)
    return repo.Tags
      |> Seq.toList
  }

  member this.LocalBranches (repository : string) = async {
    let repo = new Repository(repository)
    return repo.Branches
      |> Seq.toList
  }


  interface IGit with
    member this.Clone (url : string) (directory : string) = async {
      do! Async.SwitchToThreadPool()

      let options = new CloneOptions()
      options.IsBare <- true;
      options.OnTransferProgress <- new LibGit2Sharp.Handlers.TransferProgressHandler(fun p ->
        let message =
          "Cloning " + url + " " + p.ReceivedObjects.ToString() +
            "(" + p.IndexedObjects.ToString() + ")" + " / " + p.TotalObjects.ToString()
        console.Write(message, LoggingLevel.Trace)
        true
      )

      options.CredentialsProvider <- credentialsHandler

      Repository.Clone (url, directory, options) |> ignore
    }

    member this.ShallowClone (url : string) (directory : string) = async {
      return! (this :> IGit).Clone url directory
    }

    member this.HasCommit (gitPath : string) (revision : Revision) = async {
      do! Async.SwitchToThreadPool()
      let repo = new Repository (gitPath)
      let commit = repo.Lookup<Commit>(revision)
      return
        match commit with
        | null -> false
        | _ -> true
    }

    member this.DefaultBranch (gitPath : string) = async {
      let repo = new Repository (gitPath)
      return repo.Head.CanonicalName
    }

    member this.Unshallow (gitDir : string) = async {
      do! Async.SwitchToThreadPool()
      let repo = new Repository (gitDir)
      let options = new FetchOptions()
      options.CredentialsProvider <- credentialsHandler
      Commands.Fetch(repo, "origin", ["+refs/heads/*:refs/heads/*"; "+refs/tags/*:refs/tags/*"], options, "")
    }

    member this.UpdateRefs (gitDir : string) = (this :> IGit).Unshallow gitDir

    member this.Checkout (gitDir : string) (revision : string) = async {
      do! Async.SwitchToThreadPool()

      let repo = new Repository (gitDir)
      let options = new CheckoutOptions()

      options.OnCheckoutProgress <- new LibGit2Sharp.Handlers.CheckoutProgressHandler(fun (msg) (i) (n) ->
        let message = "Checking out " + revision +  " " + msg + " " + i.ToString() + " / " + n.ToString()
        console.Write(message, LoggingLevel.Debug)
      )

      Commands.Checkout(repo, revision, options) |> ignore
    }

    member this.CheckoutTo (gitPath : string) (revision : Revision) (installPath : string) = async {
      let! exists = Files.directoryExists (installPath)
      if not exists then
        do! Files.mkdirp installPath
        do! sharedGitClone gitPath installPath
      do! (this :> IGit).UpdateRefs installPath
      return! (this :> IGit).Checkout installPath revision
    }

    member this.FetchBranch (repository : String) (branch : Buckaroo.Branch) (_ : int) = async {
      do! Async.SwitchToThreadPool()
      let repo = new Repository (repository)
      let options = new FetchOptions()
      options.CredentialsProvider <- credentialsHandler
      Commands.Fetch(repo, "origin", [branch + ":" + branch], options, "")
    }

    member this.FetchCommits (repository : String) (branch : Buckaroo.Branch) : AsyncSeq<Revision> = asyncSeq {
      do! (this :> IGit).FetchBranch repository branch 0
      do! Async.SwitchToThreadPool()
      let repo = new Repository (repository)
      let filter = new CommitFilter()
      filter.IncludeReachableFrom <- branch
      return
        seq {
          for x in repo.Commits.QueryBy (filter) do
            yield x.Sha
        }
        |> Seq.toList
    }


    member this.RemoteRefs (url : String) = async {
      do! Async.SwitchToThreadPool()
      return Repository.ListRemoteReferences(url)
        |> Seq.filter(fun ref -> isHead ref || isTag ref)
        |> Seq.map(fun ref ->
          match isTag ref with
          | true -> {
              Type = RefType.Tag
              Revision = ref.TargetIdentifier;
              Name = ref.CanonicalName.Substring("refs/tags/".Length);
            }
          | false -> {
              Type = RefType.Branch
              Revision = ref.TargetIdentifier;
              Name = ref.CanonicalName.Substring("refs/heads/".Length);
            }
          )
        |> Seq.toList
    }

    member this.ReadFile (repository : String) (commit : Revision) (path : String) = async {
      do! Async.SwitchToThreadPool()
      let repo = new Repository (repository)
      let blob = repo.Lookup<Commit>(commit)
      let node = blob.[path].Target :?> Blob;
      return node.GetContentText()
    }
