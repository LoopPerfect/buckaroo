module Buckaroo.StartCommand

let private log (x : string) = System.Console.WriteLine(x)

let task = async {
  log("   ___           __                             ")
  log("  / _ )__ ______/ /_____ ________  ___          ")
  log(" / _  / // / __/  '_/ _ `/ __/ _ \\/ _ \\       ")
  log("/____/\\_,_/\\__/_/\\_\\\\_,_/_/  \\___/\\___/")
  log("")
  log("Buck, Buck, Buckaroo! \uD83E\uDD20")
  log("https://buckaroo.readthedocs.io/")
}
