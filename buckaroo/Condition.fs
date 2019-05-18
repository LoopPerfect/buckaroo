#nowarn "40"
namespace Buckaroo

type BinaryOperator =
| And
| Or
| Equal
| NotEqual
| Less
| LessEqual
| More
| MoreEqual
| Contains

type Expression =
| Value of FeatureValue
| Variable of string
| NotExpression of Expression
| BinaryExpression of Expression * BinaryOperator * Expression

type Condition = Expression

module ConditionParse =
  open FParsec

  let private ws = CharParsers.spaces
  let private str = CharParsers.skipString

  let private featureNameParser : Parser<string, unit> =
    CharParsers.regex @"\w[\w\d]*"

  let private valueParser = FeatureValueParse.featureValueParser
  let rec private variableParser = featureNameParser |>> Variable

  and private andParser = (str "and") >>. (Primitives.preturn And)
  and private orParser = (str "or") >>. (Primitives.preturn Or)
  and private equalParser = (str "=") >>. (Primitives.preturn Equal)
  and private notEqualParser = (str "<>") >>. (Primitives.preturn NotEqual)
  and private lessParser = (str "<") >>. (Primitives.preturn Less)
  and private lessEqualParser = (str "<=") >>. (Primitives.preturn LessEqual)
  and private moreParser = (str ">") >>. (Primitives.preturn More)
  and private moreEqualParser = (str ">=") >>. (Primitives.preturn MoreEqual)
  and private containsParser = (str "contains") >>. (Primitives.preturn Contains)

  and private binaryOperatorParser prev operators =
    Primitives.pipe2
      (ws >>. prev .>> ws)
      (Primitives.opt (Primitives.tuple2
        operators
        ((parse.Delay (fun() -> binaryOperatorParser prev operators)) <|> prev)
      ))
      (fun left rest ->
        match rest with
        | None -> left
        | Some (operator, right) -> BinaryExpression (left, operator, right)
      )

  and private compareExpressionParser =
    binaryOperatorParser
      unitParser
      (
        equalParser
        <|> notEqualParser
        <|> lessParser
        <|> lessEqualParser
        <|> moreParser
        <|> moreEqualParser
        <|> containsParser
      )

  and private notOperatorExpressionParser =
    (ws >>. (str "not") >>. ws >>. compareExpressionParser |>> NotExpression)
    <|> compareExpressionParser

  and private logicalExpressionParser =
    binaryOperatorParser
      notOperatorExpressionParser
      (
        andParser
        <|> orParser
      )

  and private binaryOperatorExpressionParser = logicalExpressionParser

  and private unitParser =
    ws >>. (
      FeatureValueParse.featureValueParser |>> Value
      <|> variableParser
      <|> (Primitives.between (str "(") (str ")") expressionParser)
    )

  and private expressionParser = parse.Delay (fun () ->
    Primitives.choice [
      binaryOperatorExpressionParser;
      unitParser;
    ])

  let conditionParser : Parser<Condition, unit> =
    expressionParser
    .>> ws
    .>> CharParsers.eof

  let parse (input : string) : Result<Condition, string> =
    match run conditionParser input with
    | Success(result, _, _) -> Result.Ok <| result
    | Failure(errorMsg, _, _) -> Result.Error errorMsg
