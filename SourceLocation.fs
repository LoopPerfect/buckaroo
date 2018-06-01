module SourceLocation

type GitLocation = { Url : string; Revision : string }

type SourceLocation = 
| Git of GitLocation
