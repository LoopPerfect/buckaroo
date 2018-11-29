namespace Buckaroo

type HttpPackageSource = {
  Url : string; 
  StripPrefix : string option; 
}

type PackageSource = 
| Http of HttpPackageSource

module PackageSource = 
  let show (x : PackageSource) = 
    match x with 
    | Http http -> 
      http.Url + 
      (http.StripPrefix |> Option.map (fun x -> "#" + x) |> Option.defaultValue "")
