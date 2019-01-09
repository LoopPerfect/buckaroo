namespace Buckaroo

type Branch = string

type Revision = string

type Tag = string

type RefType =
| Tag
| Branch

type Ref = {
  Revision : Revision
  Name : string
  Type: RefType
}

type IGit =
  abstract member Clone : string -> string -> Async<Unit>
  abstract member DefaultBranch : string -> Async<string>
  abstract member Unshallow : string -> Async<Unit>
  abstract member UpdateRefs : string -> Async<Unit>
  abstract member Checkout : string -> string -> Async<Unit>
  abstract member ShallowClone : string -> string -> Async<Unit>
  abstract member FetchBranch : string -> Branch -> Async<Unit>
  abstract member RemoteRefs : string -> Async<Ref list>
  abstract member FetchCommits : string -> Branch -> Async<Revision list>
  abstract member FetchCommit : string -> Revision -> Async<Unit>
  abstract member HasCommit : string -> Revision -> Async<bool>
  abstract member FetchFile : string -> Revision -> string -> Async<string>
  abstract member CheckoutTo : string -> Revision -> string -> Async<Unit>

module Git =

  open FSharpx
  open FParsec

  // TODO: Complete impl of following rules:
  // They can include slash / for hierarchical (directory) grouping,
  //   but no slash-separated component can begin with a dot . or end with the sequence .lock.
  // They must contain at least one /. This enforces the presence of a category like heads/, tags/ etc.
  //   but the actual names are not restricted. If the --allow-onelevel option is used, this rule is waived.
  // They cannot have two consecutive dots .. anywhere.
  // They cannot have ASCII control characters (i.e. bytes whose values are lower than \040, or \177 DEL), space, tilde ~, caret ^, or colon : anywhere.
  // They cannot have question-mark ?, asterisk *, or open bracket [ anywhere. See the --refspec-pattern option below for an exception to this rule.
  // They cannot begin or end with a slash / or contain multiple consecutive slashes (see the --normalize option below for an exception to this rule)
  // They cannot end with a dot ..
  // They cannot contain a sequence @{.
  // They cannot be the single character @.
  // They cannot contain a \.
  let branchOrTagNameParser = parse {
    let! branchOrTag = CharParsers.regex @"[a-zA-Z0-9-/\.]{2,128}"

    let invalidSequences = [ ".."; "@{"; "\\"; "//" ]

    if invalidSequences |> Seq.exists (fun x -> branchOrTag.Contains x)
      then
        let errorMessage = "Cannot contain any of: " + (invalidSequences |> String.concat ", ")
        return! fail errorMessage
    else
      return branchOrTag
  }

  let parseBranchOrTag (x : string) : Result<string, string> =
    match run branchOrTagNameParser x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(errorMsg, _, _) -> Result.Error errorMsg

