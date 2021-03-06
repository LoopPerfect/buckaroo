module Buckaroo.Paths

open System
open System.IO

let private sep = String [| Path.DirectorySeparatorChar |]

let normalize (path : string) : string =
  if path = ""
  then
    "."
  else
    let rec loop (path : string) =
      let parts =
        path.Split (Path.DirectorySeparatorChar, StringSplitOptions.None)
        |> Seq.filter (fun x -> x <> ".")
        |> Seq.toList

      match parts with
      | ".."::".."::rest ->
        ".." + sep + ".." + sep + (rest |> String.concat sep |> loop)
      | _::".."::rest ->
        rest
        |> String.concat sep
        |> loop
      | x::rest ->
        if List.isEmpty rest
        then
          x
        else
          x + sep + (rest |> String.concat sep |> loop)
      | [] -> ""
    loop path

let depth (path : string) =
  (normalize path).Split(Path.DirectorySeparatorChar).Length

let combine x y = Path.Combine (x, y) |> normalize

let rec combineAll xs =
  match xs with
  | [ x ] -> x
  | x::xs -> combine x (combineAll xs)
  | [] -> ""
