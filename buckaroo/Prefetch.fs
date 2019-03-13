module Buckaroo.Prefetch

open FSharp.Control

type private PrefetcherMessage =
| Completed
| Prefetch of PackageIdentifier

type Prefetcher (sourceExplorer : ISourceExplorer, limit : int) =
  let agent = MailboxProcessor.Start(fun inbox ->
    let rec waiting () =
      inbox.Scan (fun x ->
        match x with
        | Completed -> Some (working (limit - 1))
        | _ -> None
      )
    and working inFlightCount = async {
      while true do
        let! message = inbox.Receive ()

        return!
          match message with
          | Completed -> working (inFlightCount - 1)
          | Prefetch package ->
            async {
              try
                do!
                  sourceExplorer.FetchVersions Map.empty package
                  |> AsyncSeq.tryFirst
                  |> Async.Catch
                  |> Async.Ignore

              finally
                inbox.Post (Completed)
            }
            |> Async.Start

            if inFlightCount < limit
            then
              working (inFlightCount + 1)
            else
              waiting ()
      }

    working 0
  )

  member this.Prefetch (package) = agent.Post (Prefetch package)
