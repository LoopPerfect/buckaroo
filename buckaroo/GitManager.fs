namespace Buckaroo.Git

open System
open System.IO
open System.Security.Cryptography
open System.Text.RegularExpressions
open LibGit2Sharp
open Buckaroo

type GitManager (cacheDirectory : string) = 

  let mutable cloneCache : Map<string, Async<string>> = Map.empty

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
    let hash = 
      bytes 
      |> (new SHA256Managed()).ComputeHash 
      |> bytesToHex
    hash.Substring(0, 16) + "-" + (sanitizeFilename(url)).ToLower()

  let clone (url : string) (target : string) = async {
    "Cloning " + url + " into " + target |> Console.WriteLine
    let path = Repository.Clone(url, target)
    return path
  }

  member this.Clone (url : string) : Async<string> = async {
    match cloneCache |> Map.tryFind url with
    | Some task -> return! task
    | None -> 
      let target = Path.Combine(cacheDirectory, cloneFolderName url)
      let! task = 
        (
          if Directory.Exists target 
          then
            if Repository.IsValid(target) 
            then 
              async {
                return target
              }
            else 
              target + " is not a valid Git repository. Deleting... " |> Console.WriteLine
              Directory.Delete(target)
              clone url target
          else 
            clone url target
        )
        |> Async.StartChild
      cloneCache <- cloneCache |> Map.add url task
      return! task
  }

  member this.FetchFile (url : string) (revision : Revision) (file : string) : Async<string> = async {
    let! cloneCachePath = this.Clone url
    use repo = new Repository(cloneCachePath)
    let commit = repo.Lookup<Commit>(revision)
    return 
      match commit.[Constants.ManifestFileName] with 
      | null -> 
        new Exception(url + "#" + revision + " does not contain" + file + ". ") 
        |> raise
      | x -> 
        let blob = x.Target :?> Blob
        blob.GetContentText()
  }
