namespace Buckaroo

type Dependency = { 
  Package : PackageIdentifier; 
  Constraint : Constraint; 
  Target : Target option
}

module Dependency = 

  open FParsec

  let show (x : Dependency) = 
    (PackageIdentifier.show x.Package) + "@" + Constraint.show x.Constraint + 
      (x.Target |> Option.map Target.show |> Option.defaultValue "")

  let parser = parse {
    let! p = PackageIdentifier.parser
    do! CharParsers.skipString "@"
    let! c = Constraint.parser
    let! t = Target.parser |> Primitives.opt
    return 
      { Package = p; Constraint = c; Target = t }
  }

  let parse (x : string) : Result<Dependency, string> = 
    match run (parser .>> CharParsers.eof) x with
    | Success(result, _, _) -> Result.Ok result
    | Failure(errorMsg, _, _) -> Result.Error errorMsg
