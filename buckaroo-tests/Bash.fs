module Buckaroo.Tests.Bash

open Xunit

open Buckaroo
open System.Text

#if OS_WINDOWS

// No Bash on Windows

#else

// [<Fact>]
// let ``Bash.runBash works correctly`` () = 
//   let stdout = new StringBuilder()
//   let exitCode = 
//     Bash.runBash "hello" (stdout.Append >> ignore) ignore 
//     |> Async.RunSynchronously
//   Assert.Equal(0, exitCode)
//   Assert.Equal("Hello, world!", stdout.ToString())

[<Fact>]
let ``Bash.runBashSync works correctly`` () = 
  let stdout = new StringBuilder()
  let exitCode = 
    Bash.runBashSync "true" (stdout.Append >> ignore) ignore 
    |> Async.RunSynchronously
  Assert.Equal(0, exitCode)
  Assert.Equal("", stdout.ToString().Trim())

[<Fact>]
let ``Stress test of Bash.runBashSync works correctly`` () = 
  let task = 
    Bash.runBashSync "true" ignore ignore

  let exitCodes = 
    task 
    |> List.replicate 128
    |> Seq.chunkBySize 16
    |> Seq.map Async.Parallel
    |> Seq.collect Async.RunSynchronously
    |> Seq.toList

  Assert.True(exitCodes |> Seq.exists (fun x -> x <> 0) |> not)

#endif
