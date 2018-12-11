namespace Buckaroo

type HttpPackageSource = {
  Url : string;
  StripPrefix : string option;
  Type : ArchiveType option
}

type GitPackageSource = {
  Uri : string
}

type PackageSource =
| Git of GitPackageSource
| Http of Map<Version, HttpPackageSource>


module PackageSource =
  let show = function
  | Git g -> "git = " + g.Uri
  | Http h ->
    "{\n" + (
      h
      |> Map.toSeq
      |> Seq.map(fun (version, source) ->
          "Version = \"" + version.ToString()  + "\"" +
          "Url = \"" + source.Url + "\"\n" + "}"
        )
      |> String.concat "\n") +
    "\n}"