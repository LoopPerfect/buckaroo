module Buckaroo.SearchStrategy

type PackageConstraint = PackageIdentifier * Set<Constraint>

type LocatedVersionSet = PackageLocation * Set<Version>

type SearchStrategyError =
| LimitReached of PackageConstraint * int
| Unresolvable of PackageConstraint
| NoManifest of PackageIdentifier
| NoPrivateSolution of PackageIdentifier
| TransitiveConflict of Set<Set<PackageConstraint> * SearchStrategyError>

module SearchStrategyError =

  open System
  open Buckaroo.RichOutput

  let private showPackage p =
    p
    |> PackageIdentifier.show
    |> text
    |> foreground ConsoleColor.Blue

  let private showConstraint c =
    c
    |> Constraint.simplify
    |> Constraint.show
    |> text
    |> foreground ConsoleColor.Blue

  let private showCore (p, cs) =
    (showPackage p) + " at " + (showConstraint (All cs))

  let rec show (e : SearchStrategyError) =
    match e with
    | LimitReached ((p, c), l) ->
      "We reached the limit of " + (string l) + " consecutive failures for " +
      (showPackage p) + " at " +
      (showConstraint (All c)) + ". "
    | Unresolvable (p, c) ->
      "The package " + (showPackage p) + " at " + (showConstraint (All c)) + " is unresolvable. "
    | NoManifest p -> "We could not find any manifests for " + (showPackage p) + ". "
    | NoPrivateSolution p ->
      "We could not resolve a private dependency for " + (showPackage p) + "."
    | TransitiveConflict xs ->
      (text "We had the following conflicts: \n") +
      (
        xs
        |> Seq.collect (fun (cores, reason) ->
          cores
          |> Seq.map (fun core ->
            ("  " + (core |> showCore) + ": ") + (show reason)
          )
        )
        |> RichOutput.concat (text "\n")
      )
