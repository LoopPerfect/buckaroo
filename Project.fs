module Project

type GitHubProject = { Owner : string; Project : string }

type Project = GitHubProject

let showGitHubProject (p : GitHubProject) : string = 
  "github.com/" + p.Owner + "/" + p.Project

let show (p : Project) : string = 
  showGitHubProject p
