module ResolvedPackage

open Project
open Version

type Revision = string

type ResolvedPackage = { Project : Project; Revision : Revision; Version : Version }
