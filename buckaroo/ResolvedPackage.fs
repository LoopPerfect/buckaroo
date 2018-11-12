module Buckaroo.ResolvedPackage

open Buckaroo.Git

type ResolvedPackage = { 
  Package : PackageIdentifier; 
  Revision : Revision; 
  Version : Version; 
}
