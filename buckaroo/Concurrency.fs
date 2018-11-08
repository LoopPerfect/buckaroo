namespace Buckaroo

type ConcurrencyKey = int

type ConcurrencyManager<'T> () = 

  let createChild () = MailboxProcessor.Start(fun inbox -> async {
    while true do
      let! message = inbox.Receive()
      let (_, task, replyChannel) : (ConcurrencyKey * Async<'T> * AsyncReplyChannel<'T>) = 
        message
      let! result = task |> Async.StartChild |> Async.RunSynchronously
      replyChannel.Reply(result)
    return ()
  })

  let master = MailboxProcessor.Start(fun inbox -> async {
    let mutable children = Map.empty
    while true do
      let! message = inbox.Receive()
      let (cacheKey, _, _) = message
      let child = 
        match children |> Map.tryFind cacheKey with 
        | Some x -> x
        | None -> 
          let x = createChild()
          children <- children |> Map.add cacheKey x
          x
      child.Post message
    return ()
  })

  interface System.IDisposable with
    member this.Dispose () = 
      (master :> System.IDisposable).Dispose()

  member this.Perform cacheKey (task : Async<'T>) = async {
    return! master.PostAndAsyncReply(fun replyChannel -> 
      (cacheKey, task, replyChannel)
    )
  }

  override this.Finalize() = 
    (this :> System.IDisposable).Dispose()
