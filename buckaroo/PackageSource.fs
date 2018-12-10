namespace Buckaroo

type HttpPackageSource = {
  Url : string; 
  StripPrefix : string option; 
  Type : ArchiveType option; 
}

type GitPackageSource = {
  Uri : string; 
}

type PackageSource = 
| Http of HttpPackageSource
| Git of GitPackageSource

module PackageSource = 
  let show (x : PackageSource) = 
    match x with 
    | Http http -> 
      http.Url + 
      (http.StripPrefix |> Option.map (fun x -> "#" + x) |> Option.defaultValue "")
    | Git git -> git.Uri