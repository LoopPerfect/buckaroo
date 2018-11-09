namespace FS.INIReader 

open System
open FParsec

/// Abstract Syntax Tree of a INI file.
///    
module INIAst = 

    type INIKey = string

    type INIValue =
        | INIString of string
        | INITuple  of INIValue list 
        | INIList   of INIValue list
        | INIEmpty 

    type INIData = Map<string, Map<INIKey,INIValue>>

(* ===========  Primitive Parser  =========== *)

/// Primitive Parser
///    
module internal PrimitiveParsers =
    open INIAst 

    // Parses text surrounded by zero or more white spaces    
    let ws p = spaces >>. p .>> spaces

    let wstr t = ws (pstring t)

    let betweenSquareBrackets p =
        (wstr "[") >>. p .>> (wstr "]")

    let betweenParenthesis p =
        wstr "(" >>. p .>> wstr ")"


    let anyText s = manySatisfy (fun c -> true) s

    let identifier<'T> : Parser<string, 'T> =
         many1Satisfy2 (fun ch -> Char.IsLetter(ch)) (fun ch -> Char.IsLetterOrDigit(ch))

    let anyText2 s =
        many1Satisfy (fun ch -> (not <| Char.IsWhiteSpace(ch))
                                 && not ( ch = ')'
                                          ||  ch = '('
                                          ||  ch = ']'
                                          ||  ch = '['
                                          ||  ch = ','
                                          ||  ch = ';'
                                          ||  ch = '='
                                          )
                      ) s


    let parseQuoted<'T> : Parser<string,'T> =
        pchar '"'
        >>. manySatisfy (fun c -> c <> '"')
        .>> pchar '"'

    let extractFail p str =
        match run p str with
        | Success (result, _, _)  ->  result
        | Failure (msg,    _, _)  ->  failwith msg


    let extractOption p str =
        match run p str with
        | Success (result, _, _)  ->  Some result
        | Failure (msg,    _, _)  ->  None


    // let parseSection<'T> : Parser<string, 'T> = betweenSquareBrackets identifier


    let comment<'T> : Parser<unit, 'T> =
        pstring "#" >>. skipRestOfLine true

    let parseINIString<'T> : Parser<INIValue, 'T> =                  
        parseQuoted <|> anyText2 |>> INIString

    let parseINITuple<'T> : Parser<INIValue, 'T> =
        betweenParenthesis <| many (parseINIString .>> spaces .>> optional (pchar ',' ))
        |>> INITuple

    let parseINIList<'T> : Parser<INIValue, 'T> =
        betweenSquareBrackets <| many ( parseINIString <|> parseINITuple
                                        .>> spaces
                                        .>> optional (pchar ';' ))
        |>> INIList


    let parseINIValue<'T> : Parser<INIValue, 'T> =  
       parseINIList <|> parseINITuple
       <|> parseINIString
       <|> (skipMany comment >>. spaces |>> (fun _ -> INIEmpty))
       .>> (skipMany comment)


    let parseKV<'T> : Parser<string * INIValue, 'T> =
        anyText2 .>> wstr "=" .>>. parseINIValue

    
    let parseSection<'T> :  Parser<(string * Map<string, INIValue>),'T> =
        betweenSquareBrackets identifier
        .>>. (many (skipMany comment >>. parseKV .>> spaces)
              |>> Map.ofList)

/// High Level parser for INI files.
///     
module INIParser =
    // open FParsec
    open INIAst 
    open PrimitiveParsers
       
    let parseINI<'T> : Parser<INIData, 'T> =   many parseSection |>> Map.ofList

    /// Reads an INI AST from a string throwing an exception if it fails to parse. 
    ///
    let read: string -> INIData =
        fun s -> extractFail parseINI s 

    /// Reads an INI AST from a string returning None if it fails to parse. 
    ///  
    let read2opt: string -> INIData option =
        fun s -> extractOption parseINI s
        
    let read2res text = run parseINI text     

    /// Read an INI AST from a file, returning None if it fails to parse. 
    ///  
    let readFile: string -> INIData option =
        fun fname -> let text = System.IO.File.ReadAllText(fname)
                     in  read2opt text 

    let readFile2res fname =
        let text = System.IO.File.ReadAllText(fname)
        in read2res text 

///  Module to extract data from AST - Abstract Syntax Tree 
///   
module INIExtr =
    open INIAst

    let (>>=) ma fn = Option.bind fn ma
    
    type MaybeBuilder() =
        member this.Bind(ma, f) =
            match ma with
            | Some(a)    -> f a
            | _          -> None
        member this.Delay(f) = f()
        member this.Return(x) = Some x

    let maybe = new MaybeBuilder()

    let private parseInt (s: string): int option =
        try Some (System.Int32.Parse s) 
        with 
          :? System.FormatException -> None

    let private parseBool (s: string): bool option =
        try Some (System.Boolean.Parse s) 
        with 
          :? System.FormatException -> None

    let private parseDouble (s: string): float option =
        try Some (System.Double.Parse s) 
        with 
          :? System.FormatException -> None         

    let getINIString: INIValue -> string option =
        function
        | INIString x -> Some x
        | _           -> None

    let getINITuple: INIValue -> (INIValue list) option =
        function 
        | INITuple xs -> Some xs
        | _           -> None

    let getINIList: INIValue -> (INIValue list) option =
        function 
        | INIList xs -> Some xs
        | _          -> None

    let isINIString: INIValue -> bool =
        function
        | INIString _ -> true
        | _           -> false 

    let isINITuple: INIValue -> bool =
        function
        | INITuple _ -> true
        | _          -> false


    let isINIList: INIValue -> bool =
        function
        | INIList _ -> true
        | _         -> false 
        
    let isINIEmpty: INIValue -> bool =
        function
        | INIEmpty -> true
        | _        -> false

    // let sequence : ('a  option) list -> (list 'a) option =
    //     fun optlist -> 


    let sequence mlist =
      let (>>=) = fun ma f -> Option.bind f ma 
      let unit x  = Option.Some x  
      let mcons p q =
        p >>= fun x ->
        q >>= fun y ->
        unit (x::y)  
      List.foldBack mcons mlist (unit [])

    let applySequence fn xs =
        sequence <| List.map fn xs 
    
        
    let fieldKV: string -> string -> INIData -> INIValue option =
        fun section key ast -> ast |> Map.tryFind section
                                   |> Option.bind (Map.tryFind key)                                  

    let fieldString: string -> string -> INIData -> string option =
        fun section key ->  fieldKV section key
                            >> Option.bind getINIString
                            
    let fieldInt section key ast =
        fieldString section key ast >>= parseInt    

    let fieldDouble section key ast =
        fieldString section key ast >>= parseDouble     

    let fieldBool section key ast =
        fieldString section key ast >>= parseBool            

    /// Extracts a list of strings from an INI ast.
    ///
    /// ##Parameters 
    ///
    /// - `section` - Section to be extracted from the AST.
    /// - `key`     - key within the section to be extracted.
    ///
    let fieldListOfString (section: string) (key: string) ast =        
        ast
        |> fieldKV section key
        >>= getINIList
        >>= applySequence getINIString

    let fieldTupleOfString (section: string) (key: string) ast =
        ast
        |> fieldKV section key
        >>= getINITuple
        >>= applySequence getINIString        

    let fieldListOfTuples (section: string) (key: string) ast =
       fieldKV section key ast 
       >>= getINIList
       >>= applySequence (fun v -> getINITuple v
                                   >>= applySequence getINIString
                              )     

     
