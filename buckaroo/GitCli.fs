namespace Buckaroo

open System
open System.Text
open Buckaroo.Console
open RichOutput

type GitCli (console : ConsoleManager) =

  let log = namespacedLogger console "git"

  let nl = System.Environment.NewLine
  let runBash command = async {
    let rt =
      (
        "Running bash "
        |> RichOutput.text
        |> RichOutput.foreground ConsoleColor.Gray
      ) +
      (
        command
        |> RichOutput.text
        |> RichOutput.foreground ConsoleColor.White
      )
    console.Write (rt, LoggingLevel.Debug)
    let stdout = new StringBuilder()
    do!
      Bash.runBashSync command (stdout.Append >> ignore) ignore
      |> Async.Ignore
    return stdout.ToString()
  }

  member this.Init (directory : string) =
    runBash ("git init " + directory)

  member this.LocalTags (repository : String) = async {
    let gitDir = repository
    let command =
      "--no-pager --git-dir=" + gitDir +
      " tag"
    let! output = runBash(command)

    return
      output.Split ([| nl |], StringSplitOptions.RemoveEmptyEntries)
      |> Seq.map (fun x -> x.Trim())
      |> Seq.toList
  }

  member this.LocalBranches (repository : String) = async {
    let gitDir = repository
    let command =
      "git --no-pager --git-dir=" + gitDir +
      " branch"
    let! output = runBash(command)

    return
      output.Split ([| nl |], StringSplitOptions.RemoveEmptyEntries)
      |> Seq.map (fun x -> x.Trim())
      |> Seq.filter (fun x -> x.Contains("*") |> not)
      |> Seq.toList
  }

  interface IGit with
    member this.Clone (url : string) (directory : string) = async {
      do!
        runBash ("git clone --bare " + url + " " + directory)
        |> Async.Ignore
    }

    member this.HasCommit (gitPath : string) (revision : Revision) = async {
      let command =
        "git --no-pager --git-dir=" + gitPath +
        " log " + revision + " --pretty=format:'%H' -n0"
      let! result = runBash(command) |> Async.Catch
      return
        match result with
        | Choice1Of2 _ -> true
        | Choice2Of2 _ -> false
    }

    member this.DefaultBranch (gitPath : string) = async {
      let! result = runBash ("git --git-dir=" + gitPath + " symbolic-ref HEAD")
      return result.Trim()
    }

    member this.CheckoutTo (gitPath : string) (revision : Revision) (installPath : string) = async {
      do! Files.mkdirp installPath
      do!
        runBash ("git clone -s -n " + gitPath + " " + installPath  + " && git -C " + installPath + " checkout " + revision)
        |> Async.Ignore
    }

    member this.Unshallow (gitDir : string) = async {
      do!
        runBash ("git --git-dir=" + gitDir + " fetch --unshallow || true; git --git-dir=" + gitDir + " fetch origin '+refs/heads/*:refs/heads/*'")
        |> Async.Ignore
    }

    member this.Checkout (gitDir : string) (revision : string) = async {
      do!
        runBash ("git -C " + gitDir + " checkout " + revision + " .")
        |> Async.Ignore
    }

    member this.ShallowClone (url : String) (directory : string) = async {
      log((text "shallow cloning ") + (highlight url), LoggingLevel.Info)
      do!
        runBash ("git clone --bare --depth=1 " + url + " " + directory)
        |> Async.Ignore
    }

    member this.FetchBranch (repository : String) (branch : Branch) = async {
      let gitDir = repository
      let command =
        "git --no-pager --git-dir=" + gitDir +
        " fetch origin " + branch + ":" + branch
      do!
        runBash(command)
        |> Async.Ignore
    }

    member this.FetchCommits (repository : String) (branch : Branch) : Async<Revision list> = async {
      do! (this :> IGit).FetchBranch repository branch
      let gitDir = repository
      let command =
        "git --no-pager --git-dir=" + gitDir +
        " log " + branch + " --pretty=format:'%H'"
      let! output = runBash(command)
      return
        output.Split ([| nl |], StringSplitOptions.RemoveEmptyEntries)
        |> Seq.map (fun x -> x.Trim())
        |> Seq.toList
    }

    member this.FetchCommit (repository : String) (commit : Revision) = async {
      let gitDir = repository
      let command =
        "git --git-dir=" + gitDir +
        " fetch origin " + commit
      do!
        runBash(command)
        |> Async.Ignore
    }

    member this.FetchFile (repository : String) (commit : Revision) (path : String) = async {
      let gitDir = repository
      let command =
        "git --git-dir=" + gitDir +
        " show " + commit + ":" + path
      return! runBash(command)
    }

    member this.RemoteRefs (url : String) = async {
      let! output = runBash("git --no-pager ls-remote --heads --tags " + url)
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
                Name = name.Trim().Substring("refs/tags/".Length)
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
