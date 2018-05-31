module Version

type Branch = string

type Revision = string

type Tag = string

type Version = 
| SemVerVersion of SemVer.SemVer
| BranchVersion of Branch
| RevisionVersion of Revision
| TagVersion of Tag

let show (v : Version) : string = 
  match v with 
  | SemVerVersion semVer -> SemVer.show semVer
  | BranchVersion branch -> "branch=" + branch
  | RevisionVersion revision -> "revision=" + revision
  | TagVersion tag -> "tag=" + tag
