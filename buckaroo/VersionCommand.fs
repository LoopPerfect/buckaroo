module Buckaroo.VersionCommand

let task _ = async {
  System.Console.WriteLine (Constants.Version)

  return 0
}
