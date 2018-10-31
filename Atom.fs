module Buckaroo.Atom

type Atom = { Package : PackageIdentifier; Version : Version }

let show (a : Atom) : string = 
  PackageIdentifier.show a.Package + "@" + Version.show a.Version
