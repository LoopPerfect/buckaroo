namespace Buckaroo

type Solution = {
  Resolutions : Map<PackageIdentifier, ResolvedVersion * Solution>
}

type ResolutionStyle =
| Quick
| Upgrading

module Solution =

  let empty = {
    Resolutions = Map.empty
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
