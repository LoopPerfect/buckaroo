namespace Buckaroo

open System
open System.Text
open FSharp.Control
open FSharpx
open Buckaroo.Console
open Buckaroo.RichOutput
open Buckaroo.Bash
open Buckaroo.Logger

type GitCli (console : ConsoleManager) =

  let logger = createLogger console (Some "git")

  let nl = System.Environment.NewLine

  let runBash exe args = async {
    let rt =
      (
        "Running " + exe
        |> RichOutput.text
        |> RichOutput.foreground ConsoleColor.Gray
      ) +
      (
        args
        |> RichOutput.text
        |> RichOutput.foreground ConsoleColor.White
      )

    console.Write (rt, LoggingLevel.Debug)

    let stdout = StringBuilder ()

    do!
      Bash.runBashSync exe args (stdout.Append >> ignore) ignore
      |> Async.Ignore

    return stdout.ToString()
  }

  let listLocalCommits repository branch skip = async {
    let args =
      "--no-pager -C " + repository +
      " log " + branch + " --pretty=format:'%H'" + " --skip=" + skip.ToString()

    let! output =
      runBash "git" args
      |> Async.Catch
      |> Async.map (fun x ->
        match x with
        | Choice1Of2 y -> y
        | Choice2Of2 _ -> ""
      )

    return
      output.Split ([| nl |], StringSplitOptions.RemoveEmptyEntries)
      |> Seq.map (fun x -> x.Trim())
      |> Seq.filter (fun x -> x.Length > 0)
      |> Seq.toList
  }

  member this.Init (directory : string) =
    runBash "git" ("init " + directory)

  member this.LocalTags (repository : String) = async {
    let gitDir = repository
    let command = "--no-pager -C " + gitDir + " tag"

    let! output = runBash "git" command

    return
      output.Split ([| nl |], StringSplitOptions.RemoveEmptyEntries)
      |> Seq.map (fun x -> x.Trim())
      |> Seq.toList
  }

  member this.LocalBranches (repository : String) = async {
    let gitDir = repository
    let command =
      "--no-pager -C " + gitDir +
      " branch"
    let! output = runBash "git" command

    return
      output.Split ([| nl |], StringSplitOptions.RemoveEmptyEntries)
      |> Seq.map (fun x -> x.Trim())
      |> Seq.filter (fun x -> x.Contains("*") |> not)
      |> Seq.toList
  }

  interface IGit with
    member this.Clone (url : string) (directory : string) = async {
      do!
        runBash "git" ("clone --bare " + url + " " + directory)
        |> Async.Ignore
    }

    member this.HasCommit (gitPath : string) (revision : Revision) = async {
      let command =
        "--no-pager -C " + gitPath +
        " log " + revision + " --pretty=format:'%H' -n0"

      let! result = runBash "git" command |> Async.Catch

      return
        match result with
        | Choice1Of2 _ -> true
        | Choice2Of2 _ -> false
    }

    member this.DefaultBranch (gitPath : string) = async {
      let! result = runBash "git" ("-C " + gitPath + " symbolic-ref HEAD")

      let parts = result.Split([| '/' |])

      return parts.[2].Trim()
    }

    member this.CheckoutTo (gitPath : string) (revision : Revision) (installPath : string) = async {
      do! Files.mkdirp installPath
      do!
        runBash "git" ("clone -s -n " + gitPath + " " + installPath)
          |> Async.Ignore

      try
        do!
          runBash "git" ("-C " + installPath + " checkout " + revision)
          |> Async.Ignore
      with _ ->
        do!
          runBash "git" ("-C " + installPath + " checkout --orphan " + revision)
          |> Async.Ignore
    }

    member this.Unshallow (gitDir : string) = async {
      do!
        runBash "git" ("-C " + gitDir + " fetch --unshallow")
        |> Async.Catch
        |> Async.Ignore
      do!
        runBash "git" ("-C " + gitDir + " fetch origin +refs/heads/*:refs/heads/* +refs/tags/*:refs/tags/*")
        |> Async.Ignore
    }

    member this.UpdateRefs (gitDir : string) = async {
      do!
        runBash "git" ("-C " + gitDir + " fetch origin +refs/heads/*:refs/heads/* +refs/tags/*:refs/tags/*")
        |> Async.Ignore
    }

    member this.Checkout (gitDir : string) (revision : string) = async {
      try
        do!
          runBash "git" ("-C " + gitDir + " checkout " + revision + " .")
          |> Async.Ignore
      with
        _ -> // If the commit is an orphan then this might work
          do!
            runBash "git" ("-C " + gitDir + " checkout --orphan " + revision + " ")
            |> Async.Ignore
    }

    member this.ShallowClone (url : String) (directory : string) = async {
      logger.RichInfo ((text "Shallow cloning ") + (highlight url))
      do!
        runBash "git" ("clone --bare --depth=1 " + url + " " + directory)
        |> Async.Ignore
    }

    member this.FetchBranch (repository : String) (branch : Branch) (depth : int) = async {
      let gitDir = repository

      let fetchToDepth depth = async {
        let depthStr =
          if depth > 0
          then "--depth=" + (string depth) + " "
          else ""

        let command =
          "--no-pager -C " + gitDir +
          " fetch origin " + depthStr + branch.Trim() + ":" + branch.Trim()

        do!
          runBash "git" command
          |> Async.Ignore
      }

      try
        do! fetchToDepth depth
      with
        | :? BashException as error ->
          if error.ExitCode > 0
          then
            // Delete the branch and try again
            // Seems like commits are cached (TODO: verify this)
            do!
              runBash "git" ("-C " + gitDir + " branch -D " + branch)
              |> Async.Ignore

            do! fetchToDepth depth
    }

    member this.FetchCommits (repository : String) (branch : Branch) : AsyncSeq<Revision> = asyncSeq {
      yield!
        [0..12]
          |> Seq.map (fun i -> pown 2 i)
          |> Seq.map( fun depth skip -> async {
            let! revs =
              listLocalCommits repository branch skip

            let! fetchNext =
              (this :> IGit).FetchBranch repository branch <| (List.length revs) + depth
              |> Async.Ignore
              |> Async.StartChild

            return (revs, fetchNext)
          })
          |> AsyncSeq.ofSeq
          |> AsyncSeq.scanAsync
              (fun (skip, prev, _) next -> async {
                let! (nextList, fetchNext) = next skip
                return (skip + (nextList |> List.length), nextList, fetchNext)
              })
              ( 0, List.empty, async { return () } )
          |> AsyncSeq.takeWhile (fun (_, revs, _) -> revs.Length > 0)
          |> AsyncSeq.collect (fun (_, revs, fetchNext) -> asyncSeq {
            yield! revs |> AsyncSeq.ofSeq
            do! fetchNext
          })
    }

    member this.ReadFile (repository : String) (commit : Revision) (path : String) = async {
      let gitDir = repository
      let command =
        "-C " + gitDir +
        " show " + commit + ":" + path
      return! runBash  "git" command
    }

    member this.RemoteRefs (url : String) = async {
      let cleanUpTag (tag : string) =
        if tag.EndsWith "^{}"
        then
          tag.Substring(0, tag.Length - 3)
        else
          tag

      let! output = runBash "git" ("--no-pager ls-remote --heads --tags " + url)

      return
        output.Split ([| nl |], StringSplitOptions.RemoveEmptyEntries)
        |> Seq.choose (fun x ->
          let parts = System.Text.RegularExpressions.Regex.Split(x, @"\s+")
          match parts with
          | [| commit; name |] ->
            let isTag = name.Contains("refs/tags/")
            match isTag with
            | true -> {
                Type = RefType.Tag;
                Revision = commit;
                Name =
                  name.Trim().Substring("refs/tags/".Length)
                  |> cleanUpTag
              }
            | false -> {
                Type = RefType.Branch;
                Revision = commit;
                Name = name.Trim().Substring("refs/heads/".Length)
              }
            |> Some
          | _ -> None
        )
        |> Seq.toList
    }
