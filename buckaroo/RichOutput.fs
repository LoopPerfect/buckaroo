module Buckaroo.RichOutput

type Segment = 
  {
    Text : string;
    Foreground : System.ConsoleColor option;
    Background : System.ConsoleColor option;
  }

type RichOutput =
  {
    Segments : Segment list
  }
    static member (+) (a, b : Segment) = 
      {
        a 
          with 
            Segments = a.Segments @ [ b ]
      }
    
    static member (+) (a, b : RichOutput) = 
      {
        a 
          with 
            Segments = a.Segments @ b.Segments
      }

let zero = []

let length richOutput = 
  richOutput.Segments
  |> Seq.sumBy (fun x -> x.Text.Length)

let text s = {
  Segments = 
  [
    {
      Text = s; 
      Foreground = None; 
      Background = None; 
    }
  ]
}

let foreground color richOutput = 
  {
    richOutput
      with 
        Segments = 
          richOutput.Segments 
          |> List.map (fun x -> {
            x with Foreground = Some color; 
          })
  }

let noForeground richOutput = 
  {
    richOutput
      with 
        Segments = 
          richOutput.Segments 
          |> List.map (fun x -> {
            x with Foreground = None; 
          })
  }

let background color richOutput = 
  {
    richOutput
      with 
        Segments = 
          richOutput.Segments 
          |> List.map (fun x -> {
            x with Background = Some color; 
          })
  }

let noBackground richOutput = 
  {
    richOutput
      with 
        Segments = 
          richOutput.Segments 
          |> List.map (fun x -> {
            x with Background = None; 
          })
  }
