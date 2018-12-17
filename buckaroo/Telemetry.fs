module Buckaroo.Telemetry

open System
open System.IO
open System.Runtime
open FSharp.Data
open FSharp.Data.HttpRequestHeaders
open FSharpx

let private isEnabled () =
  Environment.GetEnvironmentVariable "BUCKAROO_TELEMETRY_OPT_OUT" = null

let private telemetryUrl = "https://analytics.buckaroo.pm"

let private isUuid (x : string) = 
  Guid.TryParse x |> fst

let readOrGenerateUser = async {
  let p = 
    Path.Combine(
      Environment.GetFolderPath Environment.SpecialFolder.Personal, 
      ".buckaroo", 
      "user.txt")
  try
    let! content = Files.readFile p
    let trimmed = content.Trim()
    if isUuid trimmed
    then
      return trimmed
    else
      File.Delete p
      let user = Guid.NewGuid() |> string
      do! Files.writeFile p user
      return user
  with _ -> 
    let user = Guid.NewGuid() |> string
    do! Files.writeFile p user
    return user
}

let private getPlatform () = 
  let parts =
    InteropServices.RuntimeInformation.OSDescription
    |> String.splitString [| "#" |] StringSplitOptions.RemoveEmptyEntries
  match parts with 
  | [| x ; _ |] -> x
  | xs -> xs |> String.concat " "

let postCommand session command = async {
  if not <| isEnabled () 
  then
    return ()
  
  let! user = readOrGenerateUser

  let json = 
    [
      "{"; 
      "  \"user\": \"" + user + "\", "; 
      "  \"session\": \"" + session + "\", "; 
      "  \"payload\": {"; 
      "    \"command\": \"" + command + "\""; 
      "  }"; 
      "}"; 
    ]
    |> String.concat (System.Environment.NewLine)

  let userAgent = 
    [
      "buckaroo"; 
      Constants.Version; 
      getPlatform (); 
      InteropServices.RuntimeInformation.OSArchitecture.ToString(); 
    ]
    |> String.concat " "
    |> String.splitString [| " " |] StringSplitOptions.RemoveEmptyEntries
    |> String.concat "-"
    |> String.toLower

  return!
    Http.AsyncRequest (
      telemetryUrl + "/logs", 
      httpMethod = "POST", 
      headers = [ 
        ContentType HttpContentTypes.Json; 
        UserAgent userAgent; 
      ], 
      body = HttpRequestBody.TextRequest json
    )
}
