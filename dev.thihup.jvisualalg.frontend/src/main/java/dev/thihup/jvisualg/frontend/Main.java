package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgLexer;
import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgParser;
import dev.thihup.jvisualg.frontend.node.Node;
import org.antlr.v4.runtime.*;

import static dev.thihup.jvisualg.frontend.node.Node.*;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

public class Main {
    public static void main(String[] args) throws Throwable {
        parse(Paths.get(args[0]));
    }

    public static VisuAlgParser.AlgorithmContext parse(Path path) throws Throwable {
        return parse(CharStreams.fromPath(path, StandardCharsets.ISO_8859_1));
    }

    public static VisuAlgParser.AlgorithmContext parse(CharStream charStream) throws Throwable {
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

    public static String javaOutput(Node node) {
        return switch (node) {
            case AlgoritimoNode(
                    String text, Node declarations, Node commands, _
            ) -> """
                    public class %s {
                        %s
                        public static void main(String[] args) {
                            %s
                        }
                    }
                    """.formatted(text.replace("\"", "").replace(" ", "_"), javaOutput(declarations), javaOutput(commands));

            case DeclarationsNode(
                    List<Node> variableDeclarationContexts, List<Node> registroDeclarationContexts,
                    List<Node> subprogramDeclarationContexts, List<Node> constantsDeclarationContexts,
                    List<Node> dosContexts, _
            ) ->
                """
                %s\
                %s\
                %s\
                %s\
                """.formatted(constantsDeclarationContexts.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")),
                        variableDeclarationContexts.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")),
                        registroDeclarationContexts.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")),
                        subprogramDeclarationContexts.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")));

            case CompundNode(
                    List<? extends Node> nodes, _
            ) -> nodes.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t"));

            case VariableDeclarationNode(String name, Node type, _) -> "%s %s;".formatted(javaOutput(type), name);

            case ConstantsDeclarationNode(List<ConstantNode> constants, _) ->
                constants.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t"));

            case ConstantNode(String name, LiteralNode value, _) -> "private static final %s %s = %s;".formatted(switch (value) {
                case IntLiteralNode _ -> "int";
                case RealLiteralNode _ -> "double";
                case StringLiteralNode _ -> "String";
                case BooleanLiteralNode _-> "boolean";
            }, name, switch (value) {
                case IntLiteralNode(int constValue, _) -> constValue;
                case RealLiteralNode(double constValue, _) -> constValue;
                case StringLiteralNode(String constValue, _) -> constValue;
                case BooleanLiteralNode(boolean constValue, _) -> constValue;
            });

            case TypeNode(String type, _) -> switch (type) {
                case "inteiro" -> "int";
                case "real" -> "double";
                case "caracter", "caractere" -> "String";
                case "logico" -> "boolean";
                default -> throw new RuntimeException("Unknown type: " + type);
            };

            case CommandsNode(List<Node> commands, _) -> commands.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t"));

            default -> {
                System.err.println("Unknown node: " + node);
                yield "";
            }
        };
    }

    static class BaseErrorListener extends org.antlr.v4.runtime.BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new RuntimeException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }

}
