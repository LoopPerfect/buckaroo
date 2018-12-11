namespace Buckaroo

type HttpPackageSource = {
  Url : string;
  StripPrefix : string option;
  Type : ArchiveType option;
  Version : Version
}

type GitPackageSource = {
  Uri : string
}

type PackageSource =
| Git of GitPackageSource
| Http of Map<Version, HttpPackageSource>
