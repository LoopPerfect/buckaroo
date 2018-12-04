namespace Buckaroo

open System
open System.IO
open System.Diagnostics
open Buckaroo.Git
open LibGit2Sharp
open System
open LibGit2Sharp

module Helpers =
  let CreateSharedGitConfig (path : string) =
    "[core]\n" +
    "  repositoryformatversion = 0\n" +
    "  filemode = true\n" +
    "  bare = false\n" +
    "  logallrefupdates = true\n" +
    "[remote \"origin\"]\n" +
    "  url =" + path + "\n"+
    "  fetch = +refs/heads/*:refs/remotes/origin/*\n"

  let SharedGitClone (src : string) (destination : string) = async {
    let gitPath = Path.Combine (destination, ".git")
    do! Files.mkdirp gitPath
    do! Files.mkdirp (Path.Combine (gitPath, "info"))
    do! Files.mkdirp (Path.Combine (gitPath, "hooks"))
    do! Files.mkdirp (Path.Combine (gitPath, "refs", "heads"))
    do! Files.mkdirp (Path.Combine (gitPath, "refs", "tags"))
    do! Files.mkdirp (Path.Combine (gitPath, "objects", "info"))
    do! Files.mkdirp (Path.Combine (gitPath, "objects", "pack"))    
    do! Files.writeFile (Path.Combine (gitPath, "description")) ""
    do! Files.writeFile (Path.Combine (gitPath, "info/exclude")) ""
    do! 
      Files.copyFile 
        (Path.Combine (src, "HEAD")) 
        (Path.Combine (gitPath, "HEAD")) 
            
    do! 
      Files.writeFile 
        (Path.Combine (gitPath, "config")) 
        (CreateSharedGitConfig src)

    do! 
      Files.writeFile 
        (Path.Combine (gitPath, "objects", "info", "alternatives")) 
        src

    return ()
  }

  

type GitLib () = 
  let nl = System.Environment.NewLine
  member this.Init (directory : string) = async {
    Repository.Init (directory, true) |> ignore
  }

  member this.LocalTags (repository : String) = async {
    let repo = new Repository(repository)
    return repo.Tags
      |> Seq.toList
  }

  member this.LocalBranches (repository : String) = async {
    let repo = new Repository(repository)
    return repo.Branches
      |> Seq.toList
  }
  

  interface IGit with 
    member this.Clone (url : string) (directory : string) = async {
      let options = new CloneOptions()
      options.IsBare <- true;
      Repository.Clone (url, directory, options) |> ignore
      return ();
    }
   
    member this.ShallowClone (url : String) (directory : string) = async {
      let options = new CloneOptions()
      options.IsBare <- true;

      options.OnTransferProgress <- new LibGit2Sharp.Handlers.TransferProgressHandler(fun p -> 
        System.Console.WriteLine ("Cloning " + url + 
          " " + p.ReceivedObjects.ToString() + "(" + p.IndexedObjects.ToString() + ")" + " / " + p.TotalObjects.ToString()
        )

        true
      )

      Repository.Clone (url, directory, options) |> ignore
      return ();
    }


    member this.DefaultBranch (gitPath : string) = async {
      let repo = new Repository (gitPath)
      return repo.Head.CanonicalName
    }

    member this.Unshallow (gitDir : string) = async {
      let repo = new Repository (gitDir);
      let options = new FetchOptions();
      Commands.Fetch(repo, "origin", ["+refs/heads/*:refs/remotes/origin/*"], options, "")
    }

    member this.Checkout (gitDir : string) (revision : string) = async {
      let repo = new Repository (gitDir);
      Commands.Checkout(repo, revision) |> ignore
    }
    member this.CheckoutTo (gitPath : string) (revision : Git.Revision) (installPath : string) = async {
      do! Files.mkdirp installPath
      do! Helpers.SharedGitClone gitPath installPath
      let repo = new Repository (installPath);
      let options = new FetchOptions();
      Console.WriteLine ("checking out: " + installPath + " " + revision)
      Commands.Fetch(repo, "origin", [], options, "")
      Commands.Checkout(repo, revision) |> ignore

      return ()
    }

    member this.FetchBranch (repository : String) (branch : Git.Branch) = async {
      let repo = new Repository (repository);
      let options = new FetchOptions();
      Commands.Fetch(repo, "origin", [branch + ":" + branch], options, "")
    }
    member this.FetchCommit (repository : String) (commit : Revision) = async {
      let repo = new Repository (repository);
      let options = new FetchOptions();
      Commands.Fetch(repo, "origin", [commit + ":" + commit], options, "")
    }
    member this.FetchCommits (repository : String) (branch : Git.Branch) : Async<Git.Revision list> = async {
      let repo = new Repository (repository)
      let filter = new CommitFilter()
      filter.IncludeReachableFrom <- branch
      return seq {
        for x in repo.Commits.QueryBy (filter) do
          yield x.Sha
      } |> Seq.toList
    }
    member this.RemoteTags (url : String) = async {
      return Repository.ListRemoteReferences(url) 
        |> Seq.filter(fun ref -> ref.CanonicalName.Contains("refs/tags/"))
        |> Seq.map(fun ref -> {
          Commit = ref.TargetIdentifier; 
          Name = ref.CanonicalName.Substring("refs/tags/".Length);
        })
        |> Seq.toList
    }

    member this.RemoteHeads (url : String) = async {
      return Repository.ListRemoteReferences(url) 
        |> Seq.filter(fun ref -> ref.CanonicalName.Contains("refs/heads/"))
        |> Seq.map(fun ref -> {
          Head = ref.TargetIdentifier; 
          Name = ref.CanonicalName.Substring("refs/heads/".Length);
        })
        |> Seq.toList
    }

    member this.FetchFile (repository : String) (commit : Revision) (path : String) = async {
      let repo = new Repository (repository)
      let blob = repo.Lookup<Commit>(commit)
      let node = blob.[path].Target :?> Blob;
      return node.GetContentText()
    }

