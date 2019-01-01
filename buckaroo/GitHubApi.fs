module Buckaroo.GitHubApi

open System
open FSharp.Data

let fetchFile (package : AdhocPackageIdentifier) (commit : Revision) (file : string) = async {
  if commit.Length <> 40
  then
    return raise <| new ArgumentException("GitHub API requires full length commit hashes")
  else
    let url =
      "https://raw.githubusercontent.com/" + package.Owner + "/" + package.Project + "/" + commit + "/" + file
    return! Http.AsyncRequestString(url)
}
