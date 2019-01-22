namespace Buckaroo

type GitPackageSource = {
  Uri : string
}

type PackageSource =
| Git of GitPackageSource
| Http of Map<Version, HttpLocation>

module PackageSource =
  let show = function
  | Git g -> "{ git = " + g.Uri + " }"
  | Http h ->
    "{\n" + (
      h
      |> Map.toSeq
      |> Seq.map(fun (version, source) ->
          "Version = \"" + version.ToString()  + "\"\n" +
          "Url = \"" + source.Url + "\"\n" +
          (source.StripPrefix
            |> Option.map(fun x -> "StripPrefix =\"" + x + "\"\n")
            |> Option.defaultValue("") ) +
          (source.Type
            |> Option.map(fun x -> "Type =\"" + x.ToString() + "\"\n")
            |> Option.defaultValue("") ) + "\n"
        )
      |> String.concat "\n") +
    "\n}"