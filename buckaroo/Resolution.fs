namespace Buckaroo

type Solution = Map<PackageIdentifier, ResolvedVersion>

type Resolution = 
| Conflict of Set<Dependency>
| Error of System.Exception
| Ok of Solution

module Resolution = 

  let show resolution = 
    match resolution with
    | Conflict xs -> "Conflict! " + (xs |> Seq.map Dependency.show |> String.concat " ")
    | Error e -> "Error! " + e.Message
    | Ok xs -> 
      "Success! " + (
        xs 
        |> Seq.map (fun x -> PackageIdentifier.show x.Key + "@" + ResolvedVersion.show x.Value) 
        |> String.concat " "
      )

  let merge (a : Resolution) (b : Resolution) : Resolution = 
    match (a, b) with 
    | (Conflict c, _) -> Conflict c
    | (_, Conflict c) -> Conflict c
    | (Error e, _) -> Error e
    | (_, Error e) -> Error e
    | (Ok x, Ok y) -> 
      let folder state (key, value) = 
        match state with 
        | Ok z -> 
          match z |> Map.tryFind key with 
          | Some v -> 
            if value = v 
            then state
            else set [] |> Resolution.Conflict // TODO
          | None -> 
            z |> Map.add key value |> Ok
        | _ -> state
      x |> Map.toSeq |> Seq.fold folder (Ok y)