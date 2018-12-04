module Buckaroo.Hashing

open System.Security.Cryptography

let private bytesToHex bytes = 
  bytes 
  |> Array.map (fun (x : byte) -> System.String.Format("{0:x2}", x))
  |> String.concat System.String.Empty

let sha256 (x : string) = 
  use hasher = new SHA256Managed()
  let bytes = System.Text.Encoding.UTF8.GetBytes x
  bytes
  |> hasher.ComputeHash 
  |> bytesToHex
