parser grammar VisuAlgParser;

options { tokenVocab=VisuAlgLexer; }

// Algorithm structure
algorithm : ALGORITMO STRING declarations INICIO commands FIM_ALGORITMO;

// Declarations
declarations : (registroDeclaration | VAR (variableDeclaration SEMICOLON?)* | subprogramDeclaration  | constantsDeclaration | dos)*;
registroDeclaration : TIPO ID EQ REGISTRO variableDeclaration* FIM_REGISTRO;
variableDeclaration :  ID (COMMA ID)* COLON type ;
constantsDeclaration: CONST (ID EQ literais SEMICOLON?)*;
type : INTEIRO | REAL | CARACTERE | LOGICO | VETOR LBRACK RANGE (COMMA RANGE)* RBRACK DE type | ID;

// Commands
commands : command*;
command : assignment
        | return  
        | readCommand
        | writeCommand
        | conditionalCommand
        | chooseCommand
        | loopCommand
        | subprogramCall
        | arquivoCommand
        | aleatorioCommand
        | timerCommand
        | pausaCommand
        | debugCommand
        | ecoCommand
        | cronometroCommand
        | limpatelaCommand
        | interrompaCommand
        | SEMICOLON
        ;

interrompaCommand : INTERROMPA;

return : RETORNE expr?;

dos : DOS;

idOrArray : ID ( (LBRACK expr (COMMA expr)* RBRACK) | (DOT ID) )*;

assignment : idOrArray ASSIGN expr;

// Subprograms
subprogramDeclaration : (procedureDeclaration | functionDeclaration);
procedureDeclaration : PROCEDIMENTO ID (LPAREN formalParameterList? RPAREN (COLON type)?)? declarations INICIO commands FIMPROCEDIMENTO;
functionDeclaration : FUNCAO ID (LPAREN? formalParameterList? RPAREN)? COLON type declarations INICIO commands FIMFUNCAO;
formalParameterList : formalParameter (SEMICOLON formalParameter)*;
formalParameter : (VAR)? ID (COMMA ID)* COLON type;

// Expressions
expr : parenExpression
     | expr (OR | XOR | AND) expr
     | expr (EQ | NEQ | LT | LE | GT | GE) expr
     | expr (ADD | SUB | MUL | DIV | MOD | DIV_INT | POW ) expr
     | NOT expr
     | SUB expr
     | atom
     ;

parenExpression: LPAREN expr RPAREN;

atom : idOrArray
     | literais
     | subprogramCall
     ;

literais :  INT_LITERAL
     | REAL_LITERAL
     | STRING
     | VERDADEIRO
     | FALSO;

exprList : expr (COMMA expr)*;

// Read command
readCommand : LEIA LPAREN exprList RPAREN;

// Write command
writeCommand : (ESCREVA | ESCREVAL) (LPAREN writeList? RPAREN)?;
writeList : writeItem (COMMA writeItem)*;
writeItem : (expr | STRING) (COLON INT_LITERAL (COLON INT_LITERAL)?)?;

// Conditional commands
conditionalCommand : SE expr* ENTAO commands FIMSE
                   | SE expr* ENTAO commands SENAO commands FIMSE
                   ;

ateCase : INT_LITERAL ATE INT_LITERAL;
ateCases: ateCase (COMMA ateCase)*;

exprOrAte : (exprList | ateCases) (COMMA exprOrAte)*;

chooseCommand: ESCOLHA expr* FACA? (CASO exprOrAte* commands)* (OUTROCASO commands)? FIMESCOLHA;

// Loop commands
loopCommand : paraCommand | enquantoCommand | repitaCommand;
paraCommand : PARA ID DE expr ATE expr? (PASSO expr)? FACA commands FIMPARA;
enquantoCommand : ENQUANTO expr FACA commands FIMENQUANTO;
repitaCommand : REPITA commands ATE expr FIMREPITA?
               | REPITA commands FIMREPITA
               ;

// Subprogram call
subprogramCall : ID (LPAREN exprList? RPAREN)?;

// Arquivo command
arquivoCommand : ARQUIVO STRING;

// Aleatorio command
aleatorioCommand : ALEATORIO (ON | OFF | INT_LITERAL (COMMA INT_LITERAL)*);

// Timer command
timerCommand : TIMER (ON | OFF | INT_LITERAL);

// Pausa command
pausaCommand : PAUSA;

// Debug command
debugCommand : DEBUG expr;

// Eco command
ecoCommand : ECO (ON | OFF);

// Cronometro command
cronometroCommand : CRONOMETRO (ON | OFF);

// Limpatela command
limpatelaCommand : LIMPATELA;
