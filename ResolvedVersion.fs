module ResolvedVersion

type Revision = string

type ResolvedVersion = { Version : Version.Version; Revision : Revision }

let isCompatible (x : ResolvedVersion) (y : ResolvedVersion) : bool = 
  x.Revision = y.Revision || x.Version = y.Version
