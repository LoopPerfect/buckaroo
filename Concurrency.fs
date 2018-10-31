namespace Buckaroo

type ConcurrencyKey = int

module Concurrency = 
  let wrapTasks<'T> (tasks : (ConcurrencyKey * Async<'T>) list) : Async<'T list> = async {
    return
      tasks 
      |> Seq.map (fun (k, t) -> t |> Async.RunSynchronously)
      |> Seq.toList
  }
