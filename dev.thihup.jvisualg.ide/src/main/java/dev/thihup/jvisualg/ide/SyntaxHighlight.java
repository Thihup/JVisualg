package dev.thihup.jvisualg.ide;

import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class SyntaxHighlight {

    private static final List<String> KEYWORDS = List.of(
            "Div", "Mod", "Se", "Entao", "Então", "Senao", "Senão", "FimSe", "Para", "De", "Ate", "Até", "Passo", "FimPara", "Faca", "Faça", "Enquanto", "FimEnquanto", "Retorne", "E", "Ou", "Nao", "Não", "Escolha", "FimEscolha", "Repita", "FimRepita", "Caso", "OutroCaso",
            "abs", "arccos", "arcsen", "arctan", "asc", "carac", "caracpnum", "compr", "copia", "cos", "cotan", "exp", "grauprad", "int", "log", "logn", "maiusc", "minusc", "numpcarac", "pos", "pi", "quad", "radpgrau", "raizq", "rand", "randi", "sen", "tan"

    );

    private static final List<String> DATA_TYPES = List.of(
            "Inteiro", "Real", "Logico", "Caracter", "Caractere", "Literal", "Vetor"
    );

    private static final List<String> SPECIAL_KEYWORDS = List.of(
            "Escreva", "Escreval", "Leia", "Algoritmo", "FimAlgoritmo", "Funcao", "Função", "FimFuncao", "FimFunção", "Procedimento", "FimProcedimento", "Inicio", "Var"
    );


    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"";
    private static final String KEYWORD_PATTERN = "(?i)\\b(" + String.join("|", KEYWORDS) + ")\\b";
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



    static StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastKwEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();
        while (matcher.find()) {
            List<String> style = switch (matcher) {

                case Matcher _ when matcher.group("KEYWORD") != null -> List.of("keyword");
                case Matcher _ when matcher.group("STRING") != null -> List.of("string");
                case Matcher _ when matcher.group("COMMENT") != null -> List.of("comment", "italic");
                case Matcher _ when matcher.group("NUMBER") != null -> List.of("number");
                case Matcher _ when matcher.group("TYPE") != null -> List.of("dataType", "underline");
                case Matcher _ when matcher.group("SPECIAL") != null -> List.of("special", "underline");
                case Matcher _ when matcher.group("BOOLEAN") != null -> List.of("number");
                default -> throw new IllegalStateException("Unexpected value: " + matcher);

            };

            spansBuilder.add(List.of(), matcher.start() - lastKwEnd);
            spansBuilder.add(style, matcher.end() - matcher.start());
            lastKwEnd = matcher.end();
        }
        spansBuilder.add(List.of(), text.length() - lastKwEnd);
        return spansBuilder.create();
    }

}
