module Buckaroo.ResolvedPackage

type ResolvedPackage = { 
  Package : PackageIdentifier; 
  Revision : Revision; 
  Version : Version; 
}
