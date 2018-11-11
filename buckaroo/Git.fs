namespace Buckaroo

type Branch = string

type Revision = string

type Tag = string

module Git = 

  open System
  open System.IO
  open System.Security.Cryptography
  open System.Text.RegularExpressions
  open LibGit2Sharp

  let bytesToHex bytes = 
    bytes 
    |> Array.map (fun (x : byte) -> System.String.Format("{0:x2}", x))
    |> String.concat System.String.Empty

  let sanitizeFilename (x : string) = 
    let regexSearch = 
      new string(Path.GetInvalidFileNameChars()) + 
      new string(Path.GetInvalidPathChars()) + 
      "@.:\\/";
    let r = new Regex(String.Format("[{0}]", Regex.Escape(regexSearch)))
    Regex.Replace(r.Replace(x, "-"), "-{2,}", "-")

  let cloneFolderName (url : string) = 
    let bytes = System.Text.Encoding.UTF8.GetBytes url
    let hash = bytes |> (new SHA256Managed()).ComputeHash |> bytesToHex
    hash.Substring(0, 16) + "-" + (sanitizeFilename(url)).ToLower()

  let clone (url : string) (target : string) = async {
    "Cloning " + url + " into " + target |> Console.WriteLine
    let path = Repository.Clone(url, target)
    return path
  }

  let ensureClone (url : string) = 
    async {
      let target = 
        url
        |> cloneFolderName 
        |> (fun x -> Path.Combine("./cache", x))
      if Directory.Exists target 
      then
        if Repository.IsValid(target) 
        then return target
        else 
          target + " is not a valid Git repository. Deleting... " |> Console.WriteLine
          Directory.Delete(target)
          return! clone url target
      else 
        return! clone url target
    }
