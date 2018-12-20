namespace Buckaroo

open System
open Buckaroo
open FSharpx

type HttpLocation = {
  Url : string;
  StripPrefix : string option;
  Type : ArchiveType option;
  Sha256 : string;
}

type Hint =
| Branch of string
| Tag of string
| Default

module Hint =
  let fromVersion (v : Version) =
    match v with
    | _ -> Hint.Default

type GitLocation = {
  Url : string;
  Hint : Hint;
  Revision : Revision;
}

// GitHub is different to Git because we can switch HTTPS & SSH easily
type HostedGitLocation = {
  Package : AdhocPackageIdentifier;
  Hint : Hint;
  Revision : Revision;
}

type PackageLocation =
| Http of HttpLocation
| Git of GitLocation
| GitHub of HostedGitLocation
| BitBucket of HostedGitLocation
| GitLab of HostedGitLocation

module PackageLocation =

  let hintToBranch (hint : Hint) =
    match hint with
    | Branch b -> Option.Some b
    | _ -> Option.None

  let gitHubUrl (x : AdhocPackageIdentifier) =
    if Environment.GetEnvironmentVariable "BUCKAROO_GITHUB_SSH" |> isNull
    then
      "https://github.com/" + x.Owner + "/" + x.Project + ".git"
    else
      "ssh://git@github.com:" + x.Owner + "/" + x.Project + ".git"

  let bitBucketUrl (x : AdhocPackageIdentifier) =
    if Environment.GetEnvironmentVariable "BUCKAROO_BITBUCKET_SSH" |> isNull
    then
      "https://bitbucket.org/" + x.Owner + "/" + x.Project + ".git"
    else
      "ssh://git@bitbucket.org:" + x.Owner + "/" + x.Project + ".git"

  let gitLabUrl (x : AdhocPackageIdentifier) =
    if Environment.GetEnvironmentVariable "BUCKAROO_GITLAB_SSH" |> isNull
    then
      "https://gitlab.com/" + x.Owner + "/" + x.Project + ".git"
    else
      "ssh://git@gitlab.com:" + x.Owner + "/" + x.Project + ".git"
  let show (x : PackageLocation) =
    match x with
    | Git g -> g.Url + "#" + g.Revision
    | Http y ->
      y.Url +
      (y.StripPrefix |> Option.map ((+) "#") |> Option.defaultValue "") +
      (y.Type |> Option.map (ArchiveType.show) |> Option.map (fun x -> "#" + x) |> Option.defaultValue "")
    | GitHub y -> (gitHubUrl y.Package) + "#" + y.Revision
    | BitBucket y -> (bitBucketUrl y.Package) + "#" + y.Revision
    | GitLab y -> (gitLabUrl y.Package) + "#" + y.Revision
