namespace Buckaroo

type Solution = {
  Resolutions : Map<PackageIdentifier, ResolvedVersion * Solution>
}

type ResolutionStyle =
| Quick
| Upgrading

type Resolution =
| Conflict of Set<Dependency * Version>
| Failure of PackageIdentifier * Constraint * System.Exception
| Error of System.Exception
| Ok of Solution

module Solution =

  let empty = {
    Resolutions = Map.empty;
  }

  type SolutionMergeError =
  | Conflict of PackageIdentifier

  let merge (a : Solution) (b : Solution) =
    let folder state (key, value) =
      match state with
      | Result.Ok solution ->
        match solution.Resolutions |> Map.tryFind key with
        | Some v ->
          if value = v
          then state
          else Result.Error (Conflict key)
        | None ->
          {
            solution with
              Resolutions =
                solution.Resolutions |> Map.add key value
          }
          |> Result.Ok
      | Result.Error _ -> state
    a.Resolutions |> Map.toSeq |> Seq.fold folder (Result.Ok b)

  let show (solution : Solution) =
    let rec f solution depth =
      let indent = "|" + ("_" |> String.replicate depth)
      solution.Resolutions
      |> Map.toSeq
      |> Seq.map (fun (k, v) ->
        let (resolvedVersion, subSolution) = v
        indent + (PackageIdentifier.show k) + "@" + (string resolvedVersion) +
          (f subSolution (depth + 1))
      )
      |> String.concat "\n"
    f solution 0

module Resolution =

  let show resolution =
    match resolution with
    | Conflict xs ->
      "Conflict! " +
      (
        xs
        |> Seq.map (fun (d, v) -> (Dependency.show d) + "->" + (Version.show v))
        |> String.concat " "
      )
    | Error e -> "Error! " + e.Message
    | Failure (package, c, e) -> "Error! " + c.ToString() + "cannot be satisfied for " + package.ToString() + " because: " + e.ToString()
    | Ok solution -> "Success! " + (Solution.show solution)

  let merge (a : Resolution) (b : Resolution) : Resolution =
    match (a, b) with
    | (Failure (x, y, z), _) -> Failure (x, y, z)
    | (_, Failure (x, y, z)) -> Failure (x, y, z)
    | (Conflict c, _) -> Conflict c
    | (_, Conflict c) -> Conflict c
    | (Error e, _) -> Error e
    | (_, Error e) -> Error e
    | (Ok x, Ok y) ->
      match Solution.merge x y with
      | Result.Ok z -> Ok z
      | Result.Error e -> Resolution.Conflict (set []) // TODO
