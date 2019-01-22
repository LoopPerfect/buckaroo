module Buckaroo.Glob

open System
open Microsoft.Extensions.FileSystemGlobbing

let private stripTrailingSlash (x : string) = 
  if x.EndsWith "/"
  then x.Substring(0, x.Length - 1)
  else x

let isLike (pattern : string) (x : string) = 
  let matcher = new Matcher(StringComparison.OrdinalIgnoreCase)
  matcher.AddInclude pattern |> ignore
  let result = matcher.Match (stripTrailingSlash x)
  result.HasMatches
