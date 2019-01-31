module Buckaroo.Tests.Command

open Xunit
open Buckaroo
open Buckaroo.Console

let private defaultLoggingLevel = LoggingLevel.Info

let private verboseLoggingLevel = LoggingLevel.Trace

let private abcDef = Adhoc { Owner = "abc"; Project = "def" }

let private ijkXyz = GitHub { Owner = "ijk"; Project = "xyz" }

[<Fact>]
let ``Command.parse works correctly`` () =
  let cases = [
    (Result.Ok (Command.Init, defaultLoggingLevel), "init");

    (Result.Ok (Command.Install, defaultLoggingLevel), "   install  ");

    (Result.Ok (Command.Resolve Quick, defaultLoggingLevel), "resolve");
    (Result.Ok (Command.Resolve Quick, verboseLoggingLevel), "resolve --verbose");
    (Result.Ok (Command.Resolve Upgrading, defaultLoggingLevel), "resolve  --upgrade    ");
    (Result.Ok (Command.Resolve Upgrading, verboseLoggingLevel), "resolve --upgrade   --verbose");

    (Result.Ok (Command.UpgradeDependencies [], defaultLoggingLevel), "upgrade");
    (Result.Ok (Command.UpgradeDependencies [ abcDef ], defaultLoggingLevel), "upgrade  abc/def");
    (Result.Ok (Command.UpgradeDependencies [], verboseLoggingLevel), "  upgrade  --verbose  ");
    (Result.Ok (Command.UpgradeDependencies [ abcDef ], verboseLoggingLevel), "upgrade  abc/def --verbose ");

    (
      Result.Ok
        (
          Command.AddDependencies
            [ { Package = ijkXyz; Constraint = Constraint.wildcard; Targets = None } ],
          defaultLoggingLevel
        ),
      "add github.com/ijk/xyz  "
    );

    (
      Result.Ok (Command.UpgradeDependencies [ abcDef; ijkXyz ], verboseLoggingLevel),
      "upgrade  abc/def github.com/ijk/xyz --verbose "
    );
  ]

  for (expected, input) in cases do
    let actual = Command.parse input

    match actual with
    | Result.Error error ->
      System.Console.WriteLine (error + "\nfor \"" + input + "\"")
    | _ -> ()

    Assert.Equal(expected, actual)
