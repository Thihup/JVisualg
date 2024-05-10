package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgLexer;
import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgParser;
import dev.thihup.jvisualg.frontend.node.Location;
import dev.thihup.jvisualg.frontend.node.Node;
import org.antlr.v4.runtime.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class VisualgParser {
    public static void main(String[] args) throws Throwable {
        parse(Files.newInputStream(Paths.get(args[0])));
    }


    public static ASTResult parse(String code) {
        return parse(new ByteArrayInputStream(code.getBytes(StandardCharsets.ISO_8859_1)));
    }
    public static ASTResult parse(InputStream code) {
        var errorListener = new BaseErrorListener();
        try {
            CharStream charStream = CharStreams.fromStream(code, StandardCharsets.ISO_8859_1);

            var lexer = new VisuAlgLexer(charStream);
            lexer.removeErrorListeners();
            lexer.addErrorListener(errorListener);

            var parser = new VisuAlgParser(new CommonTokenStream(lexer));
            parser.removeErrorListeners();
            parser.addErrorListener(errorListener);

            VisuAlgParser.AlgorithmContext ctx = parser.algorithm();
            var visitor = new VisuAlgParserVisitor();
            Node rootNode = visitor.visit(ctx);

            return new ASTResult(Optional.ofNullable(rootNode), errorListener.errors);
        } catch (Exception e) {
            return new ASTResult(Optional.empty(), errorListener.errors);
        }
    }


    static class BaseErrorListener extends org.antlr.v4.runtime.BaseErrorListener {
        final List<Error> errors = new ArrayList<>();

        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            errors.add(new Error(msg, new Location(line, charPositionInLine, line, charPositionInLine)));
        }
    }

}
