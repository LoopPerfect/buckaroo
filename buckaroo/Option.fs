module Buckaroo.Option

type OptionBuilder () = 
  member this.Bind(x, f) = 
    match x with 
    | Option.Some o -> f o
    | Option.None -> Option.None
  member this.Return(value) = Option.Some value
  member this.ReturnFrom(value) = value

let option = new OptionBuilder()

let all xs = 
  let folder = fun state next -> 
    match (state, next) with 
    | (Option.Some ys, Option.Some y) -> ys |> List.append [ y ] |> Option.Some
    | _ -> Option.None 
  xs 
  |> Seq.fold folder (Option.Some [])
