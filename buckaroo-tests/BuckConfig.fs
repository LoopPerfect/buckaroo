module Buckaroo.Tests.BuckConfig

open System
open Xunit

open Buckaroo
open FS.INIReader 

[<Fact>]
let ``.buckconfig parsing works correctly`` () =
  let content = "\n\n  \n\n \n"
  let config = INIParser.read2opt content
  let actual = config |> Option.map BuckConfig.render |> Option.bind INIParser.read2opt
  Assert.Equal(config, actual)
