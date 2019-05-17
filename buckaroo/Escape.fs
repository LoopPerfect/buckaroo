namespace Buckaroo


module Escape =
  open System.Text.RegularExpressions

  let escapeRegex = Regex(@"([\'""])")
  let escapeReplacement = "\\$1"

  let escape (quote : string) (input : string) =
    quote + escapeRegex.Replace(input, escapeReplacement) + quote

  let escapeWithoutQuotes = escape ""
  let escapeWithSingleQuotes = escape "'"
  let escapeWithDoubleQuotes = escape "\""
