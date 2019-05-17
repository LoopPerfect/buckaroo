module Buckaroo.BuckConfig

type INIKey = string

type INIValue =
  | INIString of string
  | INITuple  of INIValue list 
  | INIList   of INIValue list
  | INIEmpty 

type INIData = Map<string, Map<INIKey,INIValue>>

let rec renderValue (value : INIValue) : string = 
  match value with 
  | INIString s -> s
  | INITuple xs -> xs |> Seq.map renderValue |> String.concat " "
  | INIList xs -> xs |> Seq.map renderValue |> String.concat " "
  | INIEmpty -> ""

let renderSection (section : Map<INIKey, INIValue>) : string = 
  section
  |> Seq.map (fun kvp -> "  " + kvp.Key + " = " + renderValue kvp.Value)
  |> String.concat "\n"

let render (config : INIData) : string = 
  config 
  |> Seq.map (fun kvp -> "[" + kvp.Key + "]" + "\n" + (renderSection kvp.Value))
  |> String.concat "\n\n"
