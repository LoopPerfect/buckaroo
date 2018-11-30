module Buckaroo.Glob

open System
open Microsoft.Extensions.FileSystemGlobbing

let isLike (pattern : string) (x : string) = 
  let matcher = new Matcher(StringComparison.OrdinalIgnoreCase)
  matcher.AddInclude pattern |> ignore
  let result = matcher.Match x
  result.HasMatches
