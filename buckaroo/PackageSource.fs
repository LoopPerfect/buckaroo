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
