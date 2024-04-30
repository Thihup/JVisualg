package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgLexer;
import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgParser;
import dev.thihup.jvisualg.frontend.node.Node;
import org.antlr.v4.runtime.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Throwable {
        parse(Paths.get(args[0]));
    }

    public static VisuAlgParser.AlgorithmContext parse(Path path) throws Throwable {
        return parse(CharStreams.fromPath(path, StandardCharsets.ISO_8859_1));
    }

    public static VisuAlgParser.AlgorithmContext parse(CharStream charStream) {
        var errorListener = new BaseErrorListener();

        var lexer = new VisuAlgLexer(new LowecaseCharStream(charStream));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        var parser = new VisuAlgParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return parser.algorithm();
    }

    public static Node buildAST(VisuAlgParser.AlgorithmContext ctx) {
        var visitor = new VisuAlgParserVisitor();
        return visitor.visit(ctx);
    }


    static class BaseErrorListener extends org.antlr.v4.runtime.BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new RuntimeException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }

}
