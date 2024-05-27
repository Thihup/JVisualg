package dev.thihup.jvisualg.ide;

import de.tisoft.rsyntaxtextarea.modes.antlr.AntlrTokenMaker;
import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgLexer;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;
import org.fife.ui.rsyntaxtextarea.Token;
import org.fife.ui.rsyntaxtextarea.TokenMap;
import org.fife.ui.rsyntaxtextarea.TokenTypes;

import javax.swing.text.Segment;

public class VisualgTokenMaker extends AntlrTokenMaker {

    private static final TokenMap TOKEN_MAP = new TokenMap(true);

    static {
        SyntaxHighlight.FUNCTIONS.forEach(x -> TOKEN_MAP.put(x, TokenTypes.FUNCTION));
    }

    @Override
    public void addToken(Segment segment, int start, int end, int tokenType, int startOffset) {
        switch (tokenType) {
            case Token.IDENTIFIER:
                int value = TOKEN_MAP.get(segment, start, end);
                if (value != -1)
                    tokenType = value;
                break;
        }

        super.addToken(segment, start, end, tokenType, startOffset);
    }

    @Override
    public String[] getLineCommentStartAndEnd(int languageIndex) {
        return new String[]{"//", null};
    }

    @Override
    protected int convertType(int i) {
        return switch (i) {
            case VisuAlgLexer.ALGORITMO -> Token.RESERVED_WORD;
            case VisuAlgLexer.INICIO -> Token.RESERVED_WORD;
            case VisuAlgLexer.FIM_ALGORITMO -> Token.RESERVED_WORD;
            case VisuAlgLexer.CONST -> Token.RESERVED_WORD;
            case VisuAlgLexer.VAR -> Token.RESERVED_WORD;
            case VisuAlgLexer.INTEIRO -> Token.DATA_TYPE;
            case VisuAlgLexer.REAL -> Token.DATA_TYPE;
            case VisuAlgLexer.CARACTERE -> Token.DATA_TYPE;
            case VisuAlgLexer.LOGICO -> Token.DATA_TYPE;
            case VisuAlgLexer.VETOR -> Token.DATA_TYPE;
            case VisuAlgLexer.ESCREVA -> Token.FUNCTION;
            case VisuAlgLexer.ESCREVAL -> Token.FUNCTION;
            case VisuAlgLexer.LEIA -> Token.FUNCTION;
            case VisuAlgLexer.SE -> Token.RESERVED_WORD;
            case VisuAlgLexer.ENTAO -> Token.RESERVED_WORD;
            case VisuAlgLexer.SENAO -> Token.RESERVED_WORD;
            case VisuAlgLexer.FIMSE -> Token.RESERVED_WORD;
            case VisuAlgLexer.ESCOLHA -> Token.RESERVED_WORD;
            case VisuAlgLexer.CASO -> Token.RESERVED_WORD;
            case VisuAlgLexer.OUTROCASO -> Token.RESERVED_WORD;
            case VisuAlgLexer.FIMESCOLHA -> Token.RESERVED_WORD;
            case VisuAlgLexer.PARA -> Token.RESERVED_WORD;
            case VisuAlgLexer.DE -> Token.RESERVED_WORD;
            case VisuAlgLexer.ATE -> Token.RESERVED_WORD;
            case VisuAlgLexer.PASSO -> Token.RESERVED_WORD;
            case VisuAlgLexer.FACA -> Token.RESERVED_WORD;
            case VisuAlgLexer.FIMPARA -> Token.RESERVED_WORD;
            case VisuAlgLexer.ENQUANTO -> Token.RESERVED_WORD;
            case VisuAlgLexer.FIMENQUANTO -> Token.RESERVED_WORD;
            case VisuAlgLexer.REPITA -> Token.RESERVED_WORD;
            case VisuAlgLexer.FIMREPITA -> Token.RESERVED_WORD;
            case VisuAlgLexer.INTERROMPA -> Token.RESERVED_WORD;
            case VisuAlgLexer.PROCEDIMENTO -> Token.RESERVED_WORD;
            case VisuAlgLexer.FIMPROCEDIMENTO -> Token.RESERVED_WORD;
            case VisuAlgLexer.FUNCAO -> Token.RESERVED_WORD;
            case VisuAlgLexer.FIMFUNCAO -> Token.RESERVED_WORD;
            case VisuAlgLexer.RETORNE -> Token.RESERVED_WORD;
            case VisuAlgLexer.ARQUIVO -> Token.RESERVED_WORD;
            case VisuAlgLexer.ALEATORIO -> Token.RESERVED_WORD;
            case VisuAlgLexer.ON -> Token.RESERVED_WORD;
            case VisuAlgLexer.OFF -> Token.RESERVED_WORD;
            case VisuAlgLexer.TIMER -> Token.RESERVED_WORD;
            case VisuAlgLexer.PAUSA -> Token.RESERVED_WORD;
            case VisuAlgLexer.DEBUG -> Token.RESERVED_WORD;
            case VisuAlgLexer.ECO -> Token.RESERVED_WORD;
            case VisuAlgLexer.CRONOMETRO -> Token.RESERVED_WORD;
            case VisuAlgLexer.LIMPATELA -> Token.RESERVED_WORD;
            case VisuAlgLexer.DOS -> Token.RESERVED_WORD;
            case VisuAlgLexer.TIPO -> Token.RESERVED_WORD;
            case VisuAlgLexer.REGISTRO -> Token.RESERVED_WORD;
            case VisuAlgLexer.FIM_REGISTRO -> Token.RESERVED_WORD;
            case VisuAlgLexer.INT_LITERAL -> Token.LITERAL_NUMBER_DECIMAL_INT;
            case VisuAlgLexer.RANGE -> Token.OPERATOR;
            case VisuAlgLexer.REAL_LITERAL -> Token.LITERAL_NUMBER_FLOAT;
            case VisuAlgLexer.STRING -> Token.LITERAL_STRING_DOUBLE_QUOTE;
            case VisuAlgLexer.VERDADEIRO -> Token.LITERAL_BOOLEAN;
            case VisuAlgLexer.FALSO -> Token.LITERAL_BOOLEAN;
            case VisuAlgLexer.ADD -> Token.OPERATOR;
            case VisuAlgLexer.SUB -> Token.OPERATOR;
            case VisuAlgLexer.MUL -> Token.OPERATOR;
            case VisuAlgLexer.DIV -> Token.OPERATOR;
            case VisuAlgLexer.MOD -> Token.OPERATOR;
            case VisuAlgLexer.DIV_INT -> Token.OPERATOR;
            case VisuAlgLexer.POW -> Token.OPERATOR;
            case VisuAlgLexer.EQ -> Token.OPERATOR;
            case VisuAlgLexer.NEQ -> Token.OPERATOR;
            case VisuAlgLexer.LT -> Token.OPERATOR;
            case VisuAlgLexer.GT -> Token.OPERATOR;
            case VisuAlgLexer.LE -> Token.OPERATOR;
            case VisuAlgLexer.GE -> Token.OPERATOR;
            case VisuAlgLexer.NOT -> Token.OPERATOR;
            case VisuAlgLexer.AND -> Token.OPERATOR;
            case VisuAlgLexer.OR -> Token.OPERATOR;
            case VisuAlgLexer.XOR -> Token.OPERATOR;
            case VisuAlgLexer.ASSIGN -> Token.OPERATOR;
            case VisuAlgLexer.COLON -> Token.OPERATOR;
            case VisuAlgLexer.SEMICOLON -> Token.OPERATOR;
            case VisuAlgLexer.COMMA -> Token.OPERATOR;
            case VisuAlgLexer.LPAREN -> Token.SEPARATOR;
            case VisuAlgLexer.RPAREN -> Token.SEPARATOR;
            case VisuAlgLexer.LBRACK -> Token.SEPARATOR;
            case VisuAlgLexer.RBRACK -> Token.SEPARATOR;
            case VisuAlgLexer.DOT -> Token.OPERATOR;
            case VisuAlgLexer.ID -> Token.IDENTIFIER;
            case VisuAlgLexer.COMMENT -> Token.COMMENT_EOL;
            case VisuAlgLexer.WS -> Token.WHITESPACE;
            default -> throw new IllegalStateException();
        };
    }

    @Override
    protected Lexer createLexer(String s) {
        return new VisuAlgLexer(CharStreams.fromString(s));
    }
}
