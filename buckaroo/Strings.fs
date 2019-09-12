module Buckaroo.Strings

let replace (oldValue : string) (newValue : string) (target : string) =
  target.Replace (oldValue, newValue)
