namespace Buckaroo.Git

open System
open System.IO
open System.Security.Cryptography
open System.Text.RegularExpressions
open LibGit2Sharp
open Buckaroo

type CloneRequest = 
  | CloneRequest of string * AsyncReplyChannel<Async<string>>

type GitManager (cacheDirectory : string) = 

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

  let requestString = async {
    return System.Console.ReadLine()
  }

  let requestPassword = async {
    let mutable password = ""
    let mutable keepGoing = true
    while keepGoing do
      let key = Console.ReadKey(true);
      if key.Key <> ConsoleKey.Backspace && key.Key <> ConsoleKey.Enter
      then
        password <- password + (string key.KeyChar);
        System.Console.Write("*");
      else
        if key.Key = ConsoleKey.Backspace && password.Length > 0
        then
          password <- password.Substring(0, (password.Length - 1));
          System.Console.Write("\b \b");
        else 
          keepGoing <- key.Key <> ConsoleKey.Enter
    System.Console.Write("\n")
    return password
  }

  let clone (url : string) (target : string) = async {
    "Cloning " + url + " into " + target |> Console.WriteLine
    let options = new CloneOptions()
    let handler = Handlers.CredentialsHandler(fun url usernameFromUrl types -> 
      async {
        try
          System.Console.WriteLine("Git credentials are required for " + url + ". ")
          // Request a username
          let! username = async {
            if usernameFromUrl <> null && usernameFromUrl.Length > 0 
            then
              return usernameFromUrl
            else
              System.Console.WriteLine("Please enter your username: ")
              let mutable username = ""
              while username.Length < 1 do
                let! nextUsername = requestString
                username <- nextUsername.Trim()
              return username
          }
          // Request a password
          let mutable password = ""
          while password.Length < 1 do
            System.Console.WriteLine("Please enter a password or access-token for " + username + ": ")
            let! nextPassword = requestPassword
            password <- nextPassword.Trim()
          System.Console.WriteLine("Cheers! ")
          // Return the credentials
          let credentials = new UsernamePasswordCredentials()
          credentials.Username <- username
          credentials.Password <- password
          return credentials :> Credentials
        with error -> 
          System.Console.WriteLine(error)
          let credentials = new DefaultCredentials()
          return credentials :> Credentials
      }
      |> Async.RunSynchronously
    )
    options.CredentialsProvider <- handler
    let path = Repository.Clone(url, target, options)
    return path
  }

  let mailboxProcessor = MailboxProcessor.Start(fun inbox -> async {
    let mutable cloneCache : Map<string, Async<string>> = Map.empty
    while true do
      let! message = inbox.Receive()
      let (CloneRequest(url, repl)) = message
      match cloneCache |> Map.tryFind url with
      | Some task -> 
        repl.Reply(task)
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
        repl.Reply(task) 
  })

  member this.Clone (url : string) : Async<string> = async {
    let! res = mailboxProcessor.PostAndAsyncReply(fun ch -> CloneRequest(url, ch))
    return! res 
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
