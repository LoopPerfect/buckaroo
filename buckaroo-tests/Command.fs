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
    (Result.Ok (Command.Init, defaultLoggingLevel, RemoteFirst), "init");

    (Result.Ok (Command.Install, defaultLoggingLevel, RemoteFirst), "   install  ");

    (Result.Ok (Command.Resolve Quick, defaultLoggingLevel, RemoteFirst), "resolve");
    (Result.Ok (Command.Resolve Quick, verboseLoggingLevel, RemoteFirst), "resolve --verbose");
    (Result.Ok (Command.Resolve Upgrading, defaultLoggingLevel, RemoteFirst), "resolve  --upgrade    ");
    (Result.Ok (Command.Resolve Upgrading, verboseLoggingLevel, RemoteFirst), "resolve --upgrade  --verbose");
    (Result.Ok (Command.Resolve Quick, defaultLoggingLevel, CacheFirst), "resolve --cache-first ");
    (Result.Ok (Command.Resolve Quick, verboseLoggingLevel, CacheFirst), "resolve --cache-first --verbose");

    (Result.Ok (Command.UpgradeDependencies [], defaultLoggingLevel, RemoteFirst), "upgrade");
    (Result.Ok (Command.UpgradeDependencies [ abcDef ], defaultLoggingLevel, RemoteFirst), "upgrade  abc/def");
    (Result.Ok (Command.UpgradeDependencies [], verboseLoggingLevel, RemoteFirst), "  upgrade  --verbose  ");
    (Result.Ok (Command.UpgradeDependencies [ abcDef ], verboseLoggingLevel, RemoteFirst), "upgrade  abc/def --verbose ");
    (Result.Ok (Command.UpgradeDependencies [], verboseLoggingLevel, CacheFirst), "  upgrade  --cache-first --verbose  ");
    (Result.Ok (Command.UpgradeDependencies [ abcDef ], verboseLoggingLevel, CacheFirst), "upgrade  abc/def --cache-first --verbose ");

    (
      Result.Ok
        (
          Command.AddDependencies
            [ { Package = ijkXyz; Constraint = Constraint.wildcard; Targets = None; Features = None; Conditions = None } ],
          defaultLoggingLevel,
          RemoteFirst
        ),
      "add github.com/ijk/xyz  "
    );

    (
      Result.Ok (Command.UpgradeDependencies [ abcDef; ijkXyz ], verboseLoggingLevel, RemoteFirst),
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
