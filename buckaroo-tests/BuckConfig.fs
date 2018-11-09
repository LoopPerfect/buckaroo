module Buckaroo.Tests.BuckConfig

open System
open Xunit

open Buckaroo
open FS.INIReader 

[<Fact>]
let ``Constraint.satisfies works correctly`` () =
  let content = ""
  let config = INIParser.read2opt content
  let actual = config |> Option.map BuckConfig.render |> Option.bind INIParser.read2opt
  Assert.Equal(config, actual)
