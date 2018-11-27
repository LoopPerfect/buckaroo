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
          p.EnableRaisingEvents <- true
          p.Start() |> ignore

          System.Console.WriteLine command

          use reader = p.StandardOutput
          let stdout = reader.ReadToEnd();
          
          use errorReader = p.StandardError
          let stderr = errorReader.ReadToEnd();

          p.WaitForExit()
          if p.ExitCode > 0
          then 
            return 
              raise <| new Exception("Exit code was " + (string p.ExitCode) + "\n")

          return stdout
        with error -> 
          return raise error
      }
      |> (fun t -> Async.StartChild (t, timeout))
    return! task
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

    member this.ShallowClone (url : String) (directory : string) = async {
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

    member this.FetchCommits (repository : String) (branch : Branch) : Async<Git.Revision list> = async {
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

    member this.RemoteTags (url : String) = async {
      let! output = runBash("git --no-pager ls-remote --tags " + url)
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

    member this.RemoteHeads (url : String) = async {
      let! output = runBash("git --no-pager ls-remote --heads " + url)
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
