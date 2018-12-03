module Buckaroo.Result

type ResultBuilder () = 
  member this.Bind(x, f) = 
    match x with 
    | Result.Ok o -> f o
    | Result.Error e -> Result.Error e
  member this.Return(value) = Result.Ok value
  member this.ReturnFrom(value) = value

let result = new ResultBuilder()

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
