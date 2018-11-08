namespace Buckaroo

type ArchiveType = | Zip

module ArchiveType = 
  let show x = 
    match x with 
    | Zip -> "zip"
