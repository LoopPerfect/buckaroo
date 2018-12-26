namespace Buckaroo

type Atom = { Package : PackageIdentifier; Versions : Set<Version> }

module Atom =

  let show (a : Atom) : string =
    PackageIdentifier.show a.Package + "@" + Version.show a.Versions.MinimumElement
