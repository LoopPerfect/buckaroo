namespace Buckaroo

type Atom = { Package : PackageIdentifier; Version : Version }

module Atom = 

  let show (a : Atom) : string = 
    PackageIdentifier.show a.Package + "@" + Version.show a.Version
