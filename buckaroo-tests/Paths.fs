module Buckaroo.Tests.Paths

open System
open System.IO
open Xunit
open Buckaroo

let private sep = String [| Path.DirectorySeparatorChar |]

[<Fact>]
let ``Paths.normalize works correctly`` () =
  let cases = [
    (".", "");
    ("a", "a");
    ("a" + sep + "", "a" + sep + "");
    ("" + sep + "a" + sep + "", "" + sep + "a" + sep + "");
    ("a" + sep + " " + sep + "b", "a" + sep + " " + sep + "b");
    ("" + sep + "a" + sep + "", "" + sep + "a" + sep + "");
    ("b", "a" + sep + ".." + sep + "b");
    ("c" + sep + "d", "a" + sep + ".." + sep + "b" + sep + ".." + sep + "c" + sep + "." + sep + "." + sep + "." + sep + "d");
    (".." + sep + ".." + sep + ".." + sep + "a" + sep + "b" + sep + "c", ".." + sep + ".." + sep + ".." + sep + "a" + sep + "b" + sep + "c");
    (".." + sep + ".." + sep + ".." + sep + "a", ".." + sep + ".." + sep + ".." + sep + "." + sep + "a");
    (
      ".." + sep + ".." + sep + ".." + sep + ".." + sep + ".." + sep + ".." + sep + "buckaroo" + sep + "github" + sep + "buckaroo-pm" + sep + "pkg-config-cairo",
      ".." + sep + ".." + sep + ".." + sep + ".." + sep + ".." + sep + ".." + sep + "." + sep + "buckaroo" + sep + "github" + sep + "buckaroo-pm" + sep + "pkg-config-cairo"
    );
  ]

  for (expected, input) in cases do
    Assert.Equal(expected, Paths.normalize input)
