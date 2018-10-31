module Buckaroo.Project

// open System.IO
// open FParsec


// type GitHubProject = { Owner : string; Project : string }

// type HttpProject = { Url : string; StripPrefix : string; Type : ArchiveType }

// type ProjectSource = 
// | GitHub of GitHubProject
// | Http of HttpProject

// type Package = { Identifier : PackageIdentifier; Source : ProjectSource }

// let defaultTargets (project : Package) = 
//   [ "//:" + project.Identifier.Project ]

// let cellName (project : Package) = 
//   let identifier = project.Identifier
//   [ identifier.Owner.ToLower(); identifier.Project.ToLower() ] 
//   |> String.concat "-"

// let show (p : Package) : string = 
//   p.Identifier.Owner + "/" + p.Identifier.Project + "@" + 
//   match p.Source with
//   | GitHub x -> "github.com/" + x.Owner + "/" + x.Project
//   | Http x -> x.Url
