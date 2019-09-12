module Buckaroo.QuickstartCommand

open System
open System.Text.RegularExpressions
open Buckaroo.Tasks

let private defaultBuck (libraryName : string) =
  [
    "load('//:buckaroo_macros.bzl', 'buckaroo_deps')";
    "";
    "cxx_library(";
    "  name = '" + libraryName + "', ";
    "  header_namespace = '" + libraryName + "', ";
    "  exported_headers = subdir_glob([";
    "    ('include', '**/*.hpp'), ";
    "    ('include', '**/*.h'), ";
    "  ]), ";
    "  headers = subdir_glob([";
    "    ('private_include', '**/*.hpp'), ";
    "    ('private_include', '**/*.h'), ";
    "  ]), ";
    "  srcs = glob([";
    "    'src/**/*.cpp', ";
    "  ]), ";
    "  deps = buckaroo_deps(), ";
    "  visibility = [";
    "    'PUBLIC', ";
    "  ], ";
    ")";
    "";
    "cxx_binary(";
    "  name = 'app', ";
    "  srcs = [";
    "    'main.cpp', ";
    "  ], ";
    "  deps = [";
    "    '//:" + libraryName + "', ";
    "  ], ";
    ")";
    "";
  ]
  |> String.concat "\n"

let private defaultBuckconfig =
  [
    "[project]";
    "  ignore = .git";
    "";
    "[cxx]";
    "  should_remap_host_platform = true";
    ""
  ]
  |> String.concat "\n"

let private defaultMain =
  [
    "#include <iostream>";
    "";
    "int main() {";
    "  std::cout << \"Hello, world. \" << std::endl; ";
    "";
    "  return 0; ";
    "}";
    "";
  ]
  |> String.concat "\n"

let isValidProjectName (candidate : string) =
  (Regex(@"^[A-Za-z0-9\-_]{2,32}$")).IsMatch(candidate) &&
  candidate.ToLower () <> "app"

let requestProjectName (context : TaskContext) = async {
  let mutable candidate = ""

  while isValidProjectName candidate |> not do
    context.Console.Write("Please enter a project name (alphanumeric + underscores + dashes): ")
    let! input = context.Console.Read()
    candidate <- input.Trim()

  return candidate
}

let task (context : Tasks.TaskContext) = async {
  let! projectName = requestProjectName context

  context.Console.Write("Writing project files... ")

  do! Tasks.writeManifest Manifest.zero
  do! Files.mkdirp "src"
  do! Files.mkdirp "include"
  do! Files.mkdirp "private_include"
  do! Files.writeFile ".buckconfig" defaultBuckconfig
  do! Files.writeFile "BUCK" (defaultBuck projectName)
  do! Files.writeFile "main.cpp" defaultMain

  do! ResolveCommand.task context Solution.empty ResolutionStyle.Quick |> Async.Ignore
  do! InstallCommand.task context |> Async.Ignore

  context.Console.Write("To start your app: ")
  context.Console.Write(
    "$ buck run :app"
    |> RichOutput.text
    |> RichOutput.foreground ConsoleColor.Green
  )
  context.Console.Write("")

  return 0
}
