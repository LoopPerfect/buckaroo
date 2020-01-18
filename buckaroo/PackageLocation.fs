namespace Buckaroo

type HttpLocation = {
  Url : string
  StripPrefix : string option
  Type : ArchiveType option
}

type GitLocation = {
  Url : string
  Revision : Revision
}

type GitLabLocation = {
  Package : GitLabPackageIdentifier
  Revision : Revision
}

type HostedGitLocation = {
  Package : AdhocPackageIdentifier
  Revision : Revision
}

type PackageLocation =
| Http of HttpLocation
| Git of GitLocation
| GitHub of HostedGitLocation
| BitBucket of HostedGitLocation
| GitLab of GitLabLocation

  override this.ToString () =
    match this with
    | Http http -> http.Url + "#" + (http.StripPrefix |> Option.defaultValue "")
    | Git git -> git.Url + "#" + git.Revision
    | GitHub gitHub -> "github.com/" + gitHub.Package.Owner + "/" + gitHub.Package.Project + "#" + gitHub.Revision
    | BitBucket bitbucket -> "bitbucket.org/" + bitbucket.Package.Owner + "/" + bitbucket.Package.Project + "#" + bitbucket.Revision
    | GitLab gitLab -> "gitlab.com/" + (gitLab.Package.Groups |> String.concat "/") + "/" + gitLab.Package.Project + "#" + gitLab.Revision

module PackageLocation =

  open System

  let gitHubUrl (x : AdhocPackageIdentifier) =
    if Environment.GetEnvironmentVariable "BUCKAROO_GITHUB_SSH" |> isNull
    then
      "https://github.com/" + x.Owner + "/" + x.Project + ".git"
    else
      "git@github.com:" + x.Owner + "/" + x.Project + ".git"

  let bitBucketUrl (x : AdhocPackageIdentifier) =
    if Environment.GetEnvironmentVariable "BUCKAROO_BITBUCKET_SSH" |> isNull
    then
      "https://bitbucket.org/" + x.Owner + "/" + x.Project + ".git"
    else
      "git@bitbucket.org:" + x.Owner + "/" + x.Project + ".git"

  let gitLabUrl (x : GitLabPackageIdentifier) =
    if Environment.GetEnvironmentVariable "BUCKAROO_GITLAB_SSH" |> isNull
    then
      "https://gitlab.com/" + (x.Groups |> String.concat "/") + "/" + x.Project + ".git"
    else
      "git@gitlab.com:" + (x.Groups |> String.concat "/") + "/" + x.Project + ".git"

  let versionSetFromLocation location =
    match location with
    | PackageLocation.GitHub g -> Set [Version.Git (GitVersion.Revision g.Revision)]
    | PackageLocation.Git g -> Set [Version.Git (GitVersion.Revision g.Revision)]
    | PackageLocation.GitLab g -> Set [Version.Git (GitVersion.Revision g.Revision)]
    | PackageLocation.BitBucket g -> Set [Version.Git (GitVersion.Revision g.Revision)]
    | _ -> Set.empty
