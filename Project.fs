module Project

type GitHubProject = { Owner : string; Project : string }

type Project = 
| GitHub of GitHubProject

let sourceLocation (p : Project) = 
  match p with
  | GitHub x -> "https://github.com/" + x.Owner + "/" + x.Project + ".git"

let show (p : Project) : string = 
  match p with
  | GitHub x -> "github.com/" + x.Owner + "/" + x.Project
