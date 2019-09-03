module Buckaroo.Result

let isOk x =
  match x with
  | Ok _ -> true
  | Error _ -> false

let optionToResult error x =
  match x with
  | Some x -> Result.Ok x
  | None -> Result.Error error

let all xs =
  let folder = fun state next ->
    match (state, next) with
    | (Result.Ok ys, Result.Ok y) -> ys |> List.append [ y ] |> Result.Ok
    | (Result.Error e, _) -> Result.Error e
    | (_, Result.Error e) -> Result.Error e
  xs
  |> Seq.fold folder (Result.Ok [])
