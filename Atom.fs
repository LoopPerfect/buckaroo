module Atom

type Atom = { Project : Project.Project; Version : Version.Version }

let show (a : Atom) : string = 
  Project.show a.Project + "@" + Version.show a.Version
