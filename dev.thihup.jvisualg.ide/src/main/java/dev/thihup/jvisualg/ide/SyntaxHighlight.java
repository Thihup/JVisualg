package dev.thihup.jvisualg.ide;

import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class SyntaxHighlight {

    static final List<String> FUNCTIONS = List.of(
            "abs", "arccos", "arcsen", "arctan", "asc", "carac", "caracpnum", "compr", "copia", "cos", "cotan", "exp", "grauprad", "int", "log", "logn", "maiusc", "minusc", "mudacor", "numpcarac", "pos", "pi", "quad", "radpgrau", "raizq", "rand", "randi", "sen", "tan"
    );

    static final List<String> KEYWORDS = List.of(
            "Div", "Mod", "Se", "Entao", "Então", "Senao", "Senão", "FimSe", "Para", "De", "Ate", "Até", "Passo", "FimPara", "Faca", "Faça", "Enquanto", "FimEnquanto", "Retorne", "E", "Ou", "Nao", "Não", "Escolha", "FimEscolha", "Repita", "FimRepita", "Caso", "OutroCaso"
    );

    static final List<String> DATA_TYPES = List.of(
            "Inteiro", "Real", "Logico", "Caracter", "Caractere", "Literal", "Vetor"
    );

    static final List<String> SPECIAL_KEYWORDS = List.of(
            "Escreva", "Escreval", "Leia", "Algoritmo", "FimAlgoritmo", "Funcao", "Função", "FimFuncao", "FimFunção", "Procedimento", "FimProcedimento", "Inicio", "Var"
    );


    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String KEYWORD_PATTERN = "(?i)\\b(" + Stream.of(KEYWORDS, FUNCTIONS).flatMap(Collection::stream).collect(Collectors.joining("|")) + ")\\b";
    private static final String SPECIAL_PATTERN = "(?i)\\b(" + String.join("|", SPECIAL_KEYWORDS) + ")\\b";
    private static final String TYPES_PATTERN = "(?i)\\b(" + String.join("|", DATA_TYPES) + ")\\b";
    private static final String BOOLEAN_PATTERN = "(?i)\\b(VERDADEIRO|FALSO)\\b";
    private static final String COMMENT_PATTERN = "//[^\\n\\r]+?(?:\\*\\)|[\\n\\r])";
    private static final String NUMBER_PATTERN = "(?<![a-zA-Z])\\d+";

    private static final Pattern PATTERN = Pattern.compile(

            "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
                    + "|(?<STRING>" + STRING_PATTERN + ")"
                    + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
                    + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
                    + "|(?<BOOLEAN>" + BOOLEAN_PATTERN + ")"
                    + "|(?<TYPE>" + TYPES_PATTERN + ")"
                    + "|(?<SPECIAL>" + SPECIAL_PATTERN + ")");


}
