module Buckaroo.Tests.Command

open Xunit
open Buckaroo
open Buckaroo.Console

let private defaultLoggingLevel = LoggingLevel.Info

let private verboseLoggingLevel = LoggingLevel.Trace

[<Fact>]
let ``Command.parse works correctly`` () =
  let cases = [
    (Result.Ok (Command.Init, defaultLoggingLevel), "init");
    (Result.Ok (Command.Resolve, defaultLoggingLevel), "resolve");
    (Result.Ok (Command.Resolve, verboseLoggingLevel), "resolve --verbose");
  ]

  for (expected, input) in cases do
    Assert.Equal(expected, Command.parse input)
