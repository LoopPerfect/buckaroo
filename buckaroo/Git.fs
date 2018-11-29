namespace Buckaroo.Git

type Branch = string

type Revision = string

type Tag = string

type RemoteTag = {
  Commit : Revision; 
  Name : Tag; 
}

type RemoteBranch = {
  Head : Revision; 
  Name : Branch; 
}

type IGit = 
  abstract member Clone : string -> string -> Async<Unit>
  abstract member DefaultBranch : string -> Async<string>
  abstract member Unshallow : string -> Async<Unit>
  abstract member ShallowClone : string -> string -> Async<Unit>
  abstract member FetchBranch : string -> string -> Async<Unit>
  abstract member RemoteTags : string -> Async<RemoteTag list>
  abstract member RemoteHeads : string -> Async<RemoteBranch list>
  abstract member FetchCommits : string -> Branch -> Async<Revision list>
  abstract member FetchCommit : string -> Revision -> Async<Unit>
  abstract member FetchFile : string -> Revision -> string -> Async<string>
  abstract member CopyFromCache : string -> Revision -> string -> Async<Unit>
