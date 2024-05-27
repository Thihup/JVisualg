lexer grammar VisuAlgLexer;

options {
    caseInsensitive = true;
}

// Keywords
ALGORITMO : 'algoritmo';
INICIO : 'inicio';
FIM_ALGORITMO : 'fimalgoritmo';
CONST : 'const';
VAR : 'var';
INTEIRO : 'inteiro';
REAL : 'real' | 'numerico';
CARACTERE : 'caracter' | 'caractere';
LOGICO : 'logico';
VETOR : 'vetor';
ESCREVA : 'escreva';
ESCREVAL : 'escreval';
LEIA : 'leia';
SE : 'se';
ENTAO : 'entao' | 'então';
SENAO : 'senao' | 'senão';
FIMSE : 'fimse';
ESCOLHA : 'escolha';
CASO : 'caso';
OUTROCASO : 'outrocaso';
FIMESCOLHA : 'fimescolha';
PARA : 'para';
DE : 'de';
ATE : 'ate' | 'até';
PASSO : 'passo';
FACA : 'faca' | 'faça';
FIMPARA : 'fimpara';
ENQUANTO : 'enquanto';
FIMENQUANTO : 'fimenquanto';
REPITA : 'repita';
FIMREPITA : 'fimrepita';
INTERROMPA : 'interrompa';
PROCEDIMENTO : 'procedimento';
FIMPROCEDIMENTO : 'fimprocedimento';
FUNCAO : 'funcao' | 'função';
FIMFUNCAO : 'fimfuncao' | 'fimfunção';
RETORNE : 'retorne';
ARQUIVO : 'arquivo';
ALEATORIO : 'aleatorio' | 'aleatório';
ON : 'on';
OFF : 'off';
TIMER : 'timer';
PAUSA : 'pausa';
DEBUG : 'debug';
ECO : 'eco';
CRONOMETRO : 'cronometro';
LIMPATELA : 'limpatela';
DOS: 'dos';
TIPO : 'tipo';
REGISTRO : 'registro';
FIM_REGISTRO : 'fimregistro';

// Numeric literals
INT_LITERAL : [0-9]+;
RANGE : ([0-9]+ | ID) '..' ([0-9]+ | ID);
REAL_LITERAL : [0-9]+ '.' [0-9]*;

// String literals
STRING : '"' (~["\r\n] | '\\"' | '\\\\')* '"';

// Logical literals
VERDADEIRO : 'verdadeiro';
FALSO : 'falso';

// Operators
ADD : '+';
SUB : '-';
MUL : '*';
DIV : '/' | 'div';
MOD : '%' | 'mod';
DIV_INT : '\\';
POW : '^';
EQ : '=';
NEQ : '<>';
LT : '<';
GT : '>';
LE : '<=';
GE : '>=';
NOT : 'nao' | 'não';
AND : 'e';
OR : 'ou';
XOR : 'xou';

// Assignment
ASSIGN : ('<-' | ':=');


// Punctuation
COLON : ':';
SEMICOLON : ';';
COMMA : ',';
LPAREN : '(';
RPAREN : ')';
LBRACK : '[';
RBRACK : ']';
DOT : '.';

// Identifiers
ID : [a-z_][a-z0-9_]*;

// Comments
COMMENT : '//' ~[\r\n]* -> channel(HIDDEN);

// Whitespace
WS : [ \t\r\n]+ -> channel(HIDDEN);
