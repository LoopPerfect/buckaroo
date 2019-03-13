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
    override this.ToString() =
      this.Segments
      |> Seq.map (fun x -> x.Text)
      |> String.concat ""

    static member (+) (a, b : string) =
      {
        a
          with
            Segments =
              a.Segments
              @ [ { Text = b; Foreground = None; Background = None } ]
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

    static member (+) (a : string, b : RichOutput) =
      {
        b with
          Segments =
            [
              {
                Foreground = None
                Background = None
                Text = a
              }
            ] @ b.Segments
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

let concat sep xs =
  let rec loop sep xs =
    match xs with
    | [ x ] -> x
    | head::tail -> (head + sep + (loop sep tail))
    | [] -> text ""
  loop sep (Seq.toList xs)


let subtle = text >> (foreground System.ConsoleColor.DarkGray)
let warn = text >> (foreground System.ConsoleColor.Yellow)
let info = text >> (foreground System.ConsoleColor.Cyan)
let success = text >> (foreground System.ConsoleColor.Green)
let highlight = text >> (foreground System.ConsoleColor.White)
let identifier = text >> (foreground System.ConsoleColor.Magenta)
