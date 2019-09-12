module Buckaroo.ShowCompletions

open Buckaroo.Tasks

let task context = async {
  let completions = [
    "init";
    "help";
    "install";
    "add";
    "resolve";
    "remove";
    "upgrade";
    "version";
  ]

  context.Console.Write(completions |> String.concat " ")

  return 0
}
