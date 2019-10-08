module Buckaroo.Strings

let replace (oldValue : string) (newValue : string) (target : string) =
  target.Replace (oldValue, newValue)

let replaceAll (oldValues : string seq) (newValue : string) (target : string) =
  let rec loop oldValues target =
    match oldValues with
    | x::xs ->
      loop xs (replace x newValue target)
    | [] -> target
  loop (Seq.toList oldValues) target

let substring startIndex (target : string) =
  target.Substring startIndex

let truncate length (target : string) =
  target.Substring (0, length)
