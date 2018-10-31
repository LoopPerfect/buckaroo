namespace Buckaroo

type ISourceManager =
  abstract member FetchVersions : PackageIdentifier -> Async<Buckaroo.Version list>
  abstract member FetchLocations : PackageIdentifier -> Buckaroo.Version -> Async<PackageLocation list>
  abstract member FetchManifest : PackageLocation -> Async<Manifest>

module SourceManager = 

  open System
  open System.IO
  open System.Security.Cryptography
  open System.Text.RegularExpressions
  open LibGit2Sharp

  let sourceLocation x = 
    // "ssh://git@github.com:" + x.Owner + "/" + x.Project + ".git"
    "https://github.com/" + x.Owner + "/" + x.Project + ".git"

  let bytesToHex bytes = 
    bytes 
    |> Array.map (fun (x : byte) -> System.String.Format("{0:x2}", x))
    |> String.concat System.String.Empty

  let requestText = async {
    return! async {
      let line = Console.ReadLine()
      return line
    } |> Async.StartChild
  }

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

  let clone (url : string) (target : string) = 
    async {
      "Cloning " + url + " into " + target |> Console.WriteLine
      // let cloneOptions = new CloneOptions()
      // // cloneOptions.IsBare <- true
      // // cloneOptions.Checkout <- false
      // cloneOptions.RecurseSubmodules <- false
      // cloneOptions.CredentialsProvider <- new Handlers.CredentialsHandler(
      //   fun url user cred -> 
      //     // let credentials = new DefaultCredentials() :> Credentials
      //     let credentials = 
      //       if cred.HasFlag SupportedCredentialTypes.UsernamePassword 
      //       then 
      //         // TODO
      //         // "Please enter your credentials for " + url |> Console.WriteLine
      //         // "Username: " |> Console.WriteLine
      //         // let username = Console.ReadLine()
      //         // "Password: " |> Console.WriteLine
      //         // let password = Console.ReadLine()
      //         let username = ""
      //         let password = ""
      //         let upc = new UsernamePasswordCredentials() 
      //         upc.Username <- username
      //         upc.Password <- password
      //         upc :> Credentials
      //       else new DefaultCredentials() :> Credentials
      //     credentials)
      // cloneOptions.OnProgress <- new Handlers.ProgressHandler(fun x -> 
      //   x |> Console.WriteLine
      //   true)
      // let path = Repository.Clone(url, target, cloneOptions)
      let path = Repository.Clone(url, target)
      return path
    }

  let ensureClone (url : string) = 
    async {
      let target = 
        url
        |> cloneFolderName 
        |> (fun x -> Path.Combine("./test", x))
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

  // TODO: Support non-GitHub sources
  // Not every source-type will be Git-based
  let gitUrl (package : PackageIdentifier) : Async<string> = async {
    return 
      match package with 
      | PackageIdentifier.GitHub x -> PackageLocation.gitHubUrl x
      | _ -> new Exception("Only GitHub projects are currently supported") |> raise
  }

  let fetchVersions (package : PackageIdentifier) = 
    async {
      let! url = gitUrl package
      let! gitPath = ensureClone url 
      use repo = new Repository(gitPath)
      let references = repo.Network.ListReferences(url)
      let versions = 
        references
        |> Seq.collect (fun reference -> 
          match (reference.IsLocalBranch, reference.IsTag) with
          | (true, false) -> 
            let branch = reference.CanonicalName.Substring("refs/heads/".Length)
            let cf = new CommitFilter()
            cf.IncludeReachableFrom <- "origin/" + branch
            repo.Commits.QueryBy(cf)
              |> Seq.map (fun x -> x.Sha)
              |> Seq.distinct
              |> Seq.map Revision
              |> Seq.append [ branch |> Version.Branch ]
              |> Seq.toList
          | (false, true) -> 
            let tag = reference.CanonicalName.Substring("refs/tags/".Length)
            match SemVer.parse tag with
            | Result.Ok semVer -> 
              [
                semVer |> Version.SemVerVersion;
                tag |> Version.Tag;
              ]
            | Result.Error _ -> [ tag |> Version.Tag ]
          | _ -> [])
        |> Seq.toList
      return versions 
    }

  let fetchLocations (package : PackageIdentifier) (version : Buckaroo.Version) = 
    async {
      match package with 
      | PackageIdentifier.GitHub g -> 
        let url = PackageLocation.gitHubUrl g
        let! gitPath = ensureClone url
        use repo = new Repository(gitPath)
        return 
          match version with 
          | SemVerVersion semVer -> 
            repo.Tags 
            |> Seq.filter (fun t -> SemVer.parse t.FriendlyName = Result.Ok semVer)
            |> Seq.map (fun t -> t.Target.Sha)
            |> Seq.distinct
            |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Revision = x })
            |> Seq.toList
          | Buckaroo.Version.Branch b -> 
            let branch = 
              repo.Branches
              |> Seq.filter (fun x -> x.FriendlyName = b)
              |> Seq.item 0
            Commands.Checkout(repo, branch) |> ignore
            branch.Commits
            |> Seq.map (fun c -> c.Sha)
            |> Seq.distinct
            |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Revision = x })
            |> Seq.toList
          | Buckaroo.Version.Revision r -> 
            repo.Commits
            |> Seq.map (fun c -> c.Sha)
            |> Seq.filter (fun c -> c = r)
            |> Seq.truncate 1 
            |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Revision = x })
            |> Seq.toList
          | Buckaroo.Version.Tag tag -> 
            repo.Tags 
            |> Seq.filter (fun t -> t.FriendlyName = tag)
            |> Seq.map (fun t -> t.Target.Sha)
            |> Seq.distinct
            |> Seq.map (fun x -> PackageLocation.GitHub { Package = g; Revision = x })
            |> Seq.toList
          | Buckaroo.Version.Latest -> []
      | _ -> 
        return new Exception("Only GitHub packages are supported") |> raise
    }

  let fetchManifest (location : PackageLocation) = async {
    match location with 
    | PackageLocation.GitHub g -> 
      let url = PackageLocation.gitHubUrl g.Package
      let! gitPath = ensureClone url 
      use repo = new Repository(gitPath)
      Commands.Checkout(repo, g.Revision) |> ignore
      return 
        match repo.Head.Tip.[Constants.ManifestFileName] with 
        | null -> raise (new Exception(PackageLocation.show location + " does not contain a " + Constants.ManifestFileName + " file. "))
        | x -> 
          let blob = x.Target :?> Blob;
          let content : string = blob.GetContentText()
          match Manifest.parse content with
          | Result.Ok manifest -> manifest
          | Result.Error errorMessage -> raise (new Exception("Invalid " + Constants.ManifestFileName + " file. \n" + errorMessage))
    | _ -> 
      return new Exception("Only GitHub packages are supported") |> raise
  }

  type DefaultSourceManager() = 
    interface ISourceManager with 
      member this.FetchVersions package = fetchVersions package
      member this.FetchLocations package version = fetchLocations package version 
      member this.FetchManifest location = fetchManifest location

  // let cached (sourceManager : SourceManager) = 

  //   let cache = new ConcurrentDictionary<PackageIdentifier, Buckaroo.Version list>()
    
  //   let cachedFetchVersions (project : PackageIdentifier) = async {
  //     match cache.TryGetValue(project) with
  //     | (true, x) -> 
  //       return x
  //     | (false, _) -> 
  //       let! versions = sourceManager.FetchVersions project
  //       cache.TryAdd(project, versions) |> ignore
  //       return versions
  //   }

  //   {
  //     sourceManager with 
  //       FetchVersions = cachedFetchVersions;
  //   }
