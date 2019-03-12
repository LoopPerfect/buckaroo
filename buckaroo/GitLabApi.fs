module Buckaroo.GitLabApi

open System
open FSharp.Data

let fetchFile (package : GitLabPackageIdentifier) (commit : Revision) (file : string) = async {
  if commit.Length <> 40
  then
    return raise <| ArgumentException("GitLab API requires full length commit hashes")
  else
    let url =
      "https://gitlab.com/" + (package.Groups |> String.concat "/") +
      "/" + package.Project + "/raw/" + commit + "/" + file
    return! Http.AsyncRequestString(url)
}
