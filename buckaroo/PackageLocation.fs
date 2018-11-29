namespace Buckaroo

open Buckaroo.Git
open FSharpx

type HttpLocation = {
  Url : string; 
  StripPrefix : string option; 
  Type : ArchiveType option;
  Sha256 : string; 
}

type GitLocation = {
  Url : string; 
  Revision : Revision; 
}

// GitHub is different to Git because we can switch HTTPS & SSH easily
type GitHubLocation = {
  Package : GitHubPackageIdentifier; 
  Revision : Revision; 
}

type PackageLocation = 
| Http of HttpLocation
| Git of GitLocation
| GitHub of GitHubLocation

module PackageLocation = 

  let gitHubUrl (x : GitHubPackageIdentifier) = 
    // "ssh://git@github.com:" + x.Owner + "/" + x.Project + ".git"
    "https://github.com/" + x.Owner + "/" + x.Project + ".git"

  let show (x : PackageLocation) = 
    match x with 
    | Http y -> 
      y.Url + 
      (y.StripPrefix |> Option.map ((+) "#") |> Option.defaultValue "") + 
      (y.Type |> Option.map (fun x -> "#" + (ArchiveType.show x)) |> Option.defaultValue "") 
    | Git y -> y.Url + "#" + y.Revision
    | GitHub y -> (gitHubUrl y.Package) + "#" + y.Revision
