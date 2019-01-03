namespace Buckaroo

type Solution = {
  Resolutions : Map<PackageIdentifier, ResolvedVersion * Solution>
}

type ResolutionStyle =
| Quick
| Upgrading

type NotSatisfiable = {
  Package : PackageIdentifier
  Constraint : Constraint
  Msg : string
} with
  override this.ToString () =
    this.Constraint.ToString() +
    " cannot be satisfied for " + this.Package.ToString() +
    " because: " + this.Msg


type Resolution =
| Conflict of Set<Dependency * Version>
| Failure of NotSatisfiable
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
    | Failure f -> f.ToString()
    | Ok solution -> "Success! " + (Solution.show solution)

  let merge (a : Resolution) (b : Resolution) : Resolution =
    match (a, b) with
    | (Failure _, _) -> a
    | (_, Failure _) -> b
    | (Conflict _, _) -> a
    | (_, Conflict _) -> b
    | (Error _, _) -> a
    | (_, Error _) -> b
    | (Ok x, Ok y) ->
      match Solution.merge x y with
      | Result.Ok z -> Ok z
      | Result.Error e -> Resolution.Conflict (set []) // TODO
