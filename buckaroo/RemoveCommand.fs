module Buckaroo.RemoveCommand

open System

let task (context : Tasks.TaskContext) (packages : List<PackageIdentifier>) = async {
  Console.WriteLine(
    "Removing [ " + 
    (packages |> Seq.map PackageIdentifier.show |> String.concat " ") + 
    " ]... "
  )

  let (_, sourceExplorer) = context

  let! manifest = Tasks.readManifest

  let newManifest =
    packages
    |> Seq.fold (fun state next -> Manifest.remove state next) manifest
  
  if manifest = newManifest 
  then
    Console.WriteLine("No changes were made. ")
  else
    let! maybeLock = Tasks.readLockIfPresent
    let! resolution = Solver.solve sourceExplorer newManifest ResolutionStyle.Quick maybeLock 

    match resolution with
    | Ok solution -> 
      let newLock = Lock.fromManifestAndSolution newManifest solution
      
      let lock = 
        maybeLock 
        |> Option.defaultValue { newLock with Packages = Map.empty }
      
      Console.WriteLine(Lock.showDiff lock newLock)

      let removedPackages = 
        lock.Packages
        |> Map.toSeq
        |> Seq.map (fun (package, _) -> package)
        |> Seq.filter (fun x -> newLock.Packages |> Map.containsKey x |> not)

      for removedPackage in removedPackages do 
        let path = InstallCommand.packageInstallPath removedPackage
        Console.WriteLine("Deleting " + path + "... ")
        Files.deleteDirectoryIfExists path |> ignore

      do! Tasks.writeManifest newManifest
      do! Tasks.writeLock newLock
      do! InstallCommand.task context

      Console.WriteLine("Done. ")

    | x -> 
      Console.WriteLine(x)
      Console.WriteLine("No changes were written. ")
}
