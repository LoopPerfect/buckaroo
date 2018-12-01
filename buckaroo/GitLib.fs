namespace Buckaroo

open System
open System.IO
open System.Diagnostics
open Buckaroo.Git
open LibGit2Sharp
open System
open LibGit2Sharp

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

    member this.DefaultBranch (gitPath : string) = async {
      let repo = new Repository (gitPath)
      return repo.Head.CanonicalName
    }

    member this.CheckoutTo (gitPath : string) (revision : Git.Revision) (installPath : string) = async {
      do! Files.mkdirp installPath
      let options = new RepositoryOptions()

      options.IndexPath <- Path.Combine (installPath, ".git/index")
      options.WorkingDirectoryPath <- gitPath
      Repository.Init(installPath) |> ignore
      let repo = new Repository(installPath)
      Commands.Checkout( repo, revision , new CheckoutOptions()) |> ignore
      return ()
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

    member this.ShallowClone (url : String) (directory : string) = async {
      do! this.Init (directory)
      let repo = new Repository (directory)
      repo.Network.Remotes.Add("origin", url) |> ignore
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

