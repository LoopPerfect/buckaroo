namespace Buckaroo

open System
open System.IO
open System.Diagnostics
open Buckaroo.Git

type GitCli () = 

  let nl = System.Environment.NewLine

  let runBash (command : String) = async {
    let timeout = 3 * 60 * 1000
    
    let! task = 
      async {
        if command.Contains("\"") || command.Contains("$") 
        then 
          return 
            raise <| new Exception("Malicious bash? " + command)
        try 
          // System.Console.WriteLine("  " + command)

          let startInfo = new ProcessStartInfo()

          startInfo.CreateNoWindow <- true
          startInfo.UseShellExecute <- false
          startInfo.FileName <- "/bin/bash"
          startInfo.Arguments <- "-c \"" + command + "\""
          startInfo.RedirectStandardOutput <- true
          startInfo.RedirectStandardError <- true
          startInfo.RedirectStandardInput <- true
          startInfo.WindowStyle <- ProcessWindowStyle.Hidden

          let p = new Process()

          p.StartInfo <- startInfo

          p.Start() |> ignore
          p.WaitForExit()

          let standardOutput = p.StandardOutput.ReadToEnd()

          if p.ExitCode > 0
          then 
            let standardError = p.StandardError.ReadToEnd()

            return 
              raise <| new Exception("Exit code was " + (string p.ExitCode) + "\n" + standardError)

          return standardOutput
        with error -> 
          return raise error
      }
      |> (fun t -> Async.StartChild (t, timeout))
    return! task
  }

  member this.Init (directory : string) = 
    runBash ("git init " + directory)

  member this.LocalTags (repository : String) = async {
    let gitDir = Path.Combine(repository, "./.git")
    let command =
      "git --git-dir=" + gitDir + 
      " --work-tree=" + repository + 
      " tag"
    let! output = runBash(command)
    return
      output.Split ([| nl |], StringSplitOptions.RemoveEmptyEntries)
      |> Seq.map (fun x -> x.Trim())
      |> Seq.toList
  }

  member this.LocalBranches (repository : String) = async {
    let gitDir = Path.Combine(repository, "./.git")
    let command =
      "git --git-dir=" + gitDir + 
      " --work-tree=" + repository + 
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
        runBash ("git clone " + url + " " + directory)
        |> Async.Ignore
    }

    member this.ShallowClone (url : String) (directory : string) = async {
      do! 
        runBash ("git clone --depth=1 " + url + " " + directory)
        |> Async.Ignore
    }

    member this.FetchBranch (repository : String) (branch : Branch) = async {
      let gitDir = Path.Combine(repository, "./.git")
      let command =
        "git --git-dir=" + gitDir + 
        " --work-tree=" + repository + 
        " fetch origin " + branch
      do! 
        runBash(command)
        |> Async.Ignore
    }

    member this.FetchCommits (repository : String) (branch : Branch) : Async<Git.Revision list> = async {
      do! (this :> IGit).FetchBranch repository branch
      let gitDir = Path.Combine(repository, "./.git")
      let command = 
        "git --git-dir=" + gitDir + 
        " --work-tree=" + repository + 
        " log origin/" + branch + " --pretty=format:'%H'"
      let! output = runBash(command)
      return 
        output.Split ([| nl |], StringSplitOptions.RemoveEmptyEntries)
        |> Seq.map (fun x -> x.Trim())
        |> Seq.toList
    }

    member this.RemoteTags (url : String) = async {
      let! output = runBash("git ls-remote --tags " + url)
      return
        output.Split ([| nl |], StringSplitOptions.RemoveEmptyEntries)
        |> Seq.choose (fun x -> 
          let parts = System.Text.RegularExpressions.Regex.Split(x, @"\s+")
          match (parts) with 
          | [| commit; name |] -> 
            let cleanedName = name.Trim().Substring("refs/tags/".Length)
            {
              Commit = commit.Trim(); 
              Name = 
                if cleanedName.EndsWith("^{}")
                then cleanedName.Substring(0, cleanedName.Length - "^{}".Length)
                else cleanedName;
            }
            |> Some
          | _ -> None
        )
        |> Seq.toList
    }

    member this.FetchCommit (repository : String) (commit : Revision) = async {
      let gitDir = Path.Combine(repository, "./.git")
      let command =
        "git --git-dir=" + gitDir + 
        " --work-tree=" + repository + 
        " fetch origin " + commit
      do! 
        runBash(command)
        |> Async.Ignore
    }

    member this.FetchFile (repository : String) (commit : Revision) (path : String) = async {
      let gitDir = Path.Combine(repository, "./.git")
      let command =
        "git --git-dir=" + gitDir + 
        " --work-tree=" + repository + 
        " show " + commit + ":" + path
      return! runBash(command)
    }

    member this.RemoteHeads (url : String) = async {
      let! output = runBash("git ls-remote --heads " + url)
      return
        output.Split ([| nl |], StringSplitOptions.RemoveEmptyEntries)
        |> Seq.choose (fun x -> 
          let parts = System.Text.RegularExpressions.Regex.Split(x, @"\s+")
          match parts with 
          | [| commit; name |] -> 
            {
              Head = commit.Trim(); 
              Name = name.Substring("refs/heads/".Length).Trim();
            }
            |> Some
          | _ -> None
        )
        |> Seq.toList
    }
