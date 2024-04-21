package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgLexer;
import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgParser;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Throwable {
        parse(Paths.get(args[0]));
    }

    public static VisuAlgParser.AlgorithmContext parse(Path path) throws Throwable {
        var errorListener = new BaseErrorListener();

        var lexer = new VisuAlgLexer(new LowecaseCharStream(CharStreams.fromPath(path, StandardCharsets.ISO_8859_1)));
        lexer.removeErrorListeners();
        lexer.addErrorListener(errorListener);

        var parser = new VisuAlgParser(new CommonTokenStream(lexer));
        parser.removeErrorListeners();
        parser.addErrorListener(errorListener);

        return parser.algorithm();
    }

    static class BaseErrorListener extends org.antlr.v4.runtime.BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new RuntimeException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }

}
