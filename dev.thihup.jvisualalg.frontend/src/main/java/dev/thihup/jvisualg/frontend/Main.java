package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgLexer;
import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgParser;
import dev.thihup.jvisualg.frontend.node.Location;
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

    public static String javaOutput(Node node) {
        return switch (node) {
            case null -> "// to be implemented (null?)";
            case AlgoritimoNode(
                    String text, Node declarations, Node commands, _
            ) -> """
                    // %s
                    %s
                    void main() {
                        %s
                    }
                    """.formatted(text, javaOutput(declarations), javaOutput(commands));

            case DeclarationsNode(
                    List<Node> variableDeclarationContexts, List<Node> registroDeclarationContexts,
                    List<Node> subprogramDeclarationContexts, List<Node> constantsDeclarationContexts,
                    _, _
            ) ->
                """
                %s
                %s
                %s
                %s
                """.formatted(constantsDeclarationContexts.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")),
                        variableDeclarationContexts.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")),
                        registroDeclarationContexts.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")),
                        subprogramDeclarationContexts.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")));


            case CompundNode(
                    List<? extends Node> nodes, _
            ) -> nodes.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t"));

            case VariableDeclarationNode(String name, ArrayTypeNode type, _) ->
                    "%s%s %s = new %s%s;".formatted(javaOutput(type.type()), "[]".repeat((int)type.dimensions()), name, javaOutput(type.type()), type.sizes().stream().map(Main::javaOutput).collect(Collectors.joining("][", "[", "]")));

            case VariableDeclarationNode(String name, Node type, _) -> "%s %s;".formatted(javaOutput(type), name);

            case ConstantsDeclarationNode(List<ConstantNode> constants, _) ->
                constants.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t"));

            case ConstantNode(String name, IntLiteralNode(int value, _), _) -> "private static final int %s = %s;".formatted(name, value);
            case ConstantNode(String name, RealLiteralNode(double value, _), _) -> "private static final double %s = %s;".formatted(name, value);
            case ConstantNode(String name, StringLiteralNode(String value, _), _) -> "private static final String %s = %s;".formatted(name, value);
            case ConstantNode(String name, BooleanLiteralNode(boolean value, _), _) -> "private static final boolean %s = %s;".formatted(name, value);


            case TypeNode(String type, _) -> switch (type.toLowerCase()){
                case "inteiro" -> "int";
                case "real" -> "double";
                case "caracter", "caractere", "literal" -> "String";
                case "logico" -> "boolean";
                default -> type;
            };

            case CommandsNode(List<Node> commands, _) -> commands.stream().map(Main::javaOutput).map("\t"::concat).collect(Collectors.joining("\n\t"));

            case CommandNode(Node command, _) -> javaOutput(command);

            case InterrompaCommandNode(_) -> "break;";

            case ReturnNode(Node expr, _) -> "return %s;".formatted(javaOutput(expr));

            case DosNode(_) -> "";

            case AssignmentNode(Node idOrArray, Node expr, _) ->
                "%s = %s;".formatted(javaOutput(idOrArray), javaOutput(expr));

            case IncrementNode(Node expr, Node value, _) -> "%s += %s".formatted(javaOutput(expr), javaOutput(value));

            case ForCommandNode(Node start, Node test, Node end, List<Node> commands, _) -> {
                String s = start != null ? "int " + javaOutput(start) : ";";
                yield """
                    for (%s %s; %s) {
                        %s
                    }
                    """.formatted(s, javaOutput(test), javaOutput(end), commands.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")));
            }

            case WhileCommandNode(Node condition, List<Node> commands, boolean atTheEnd, _) -> {
                if (atTheEnd) {
                    yield """
                        do {
                            %s
                        } while (%s);
                        """.formatted(commands.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")), javaOutput(condition));
                } else {
                    yield """
                        while (%s) {
                            %s
                        }
                        """.formatted(javaOutput(condition), commands.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")));
                }
            }

            case WriteCommandNode(boolean newLine, List<Node> writeList, _) -> {
                if (newLine) {
                    yield "System.out.println(%s);".formatted(writeList.stream().map(Main::javaOutput).collect(Collectors.joining(" + ")));
                } else {
                    yield "System.out.print(%s);".formatted(writeList.stream().map(Main::javaOutput).collect(Collectors.joining(" + ")));
                }
            }

            case WriteItemNode(Node expr, Integer spaces, Integer precision, _) -> {
                if (spaces != null && precision != null) {
                    yield "String.format(\"%%%d.%df\", %s)".formatted(spaces, precision, javaOutput(expr));
                } else if (spaces != null) {
                    yield "String.format(\"%%%d\", %s)".formatted(spaces, javaOutput(expr));
                } else if (precision != null) {
                    yield "String.format(\"%%.%df\", %s)".formatted(precision, javaOutput(expr));
                } else {
                    yield javaOutput(expr);
                }
            }

            case ConditionalCommandNode(
                    Node condition, List<Node> thenCommands, List<Node> elseCommands, Location _
            ) -> {
                if (elseCommands.isEmpty()) {
                    yield """
                        if (%s) {
                            %s
                        }
                        """.formatted(javaOutput(condition), thenCommands.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")));
                } else {
                    yield """
                        if (%s) {
                            %s
                        } else {
                            %s
                        }
                        """.formatted(javaOutput(condition), thenCommands.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")), elseCommands.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")));
                }
            }

            case RegistroDeclarationNode(String name, List<Node> variableDeclarationContexts, _) ->
                    """
                    class %s {
                        %s
                    }
                    """.formatted(name, variableDeclarationContexts.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")));

            case IntLiteralNode(int value, _) -> String.valueOf(value);

            case RealLiteralNode(double value, _) -> String.valueOf(value);

            case StringLiteralNode(String value, _) ->  value;

            case BooleanLiteralNode(boolean value, _) -> String.valueOf(value) ;

            case IdNode(String id, _) -> id;

            case ArrayAccessNode(IdNode id, List<Node> indexes, _) -> "%s[%s-1]".formatted(Main.javaOutput(id), indexes.stream().map(Main::javaOutput).collect(Collectors.joining("][")));


            case SubprogramCallNode(String name, List<Node> args, Location location) ->
                    "%s(%s)".formatted(name, args.stream().map(Main::javaOutput).collect(Collectors.joining(", ")));

            case FunctionDeclarationNode(String name, Node type, List<Node> args, List<Node> declarations, List<Node> commands, _) ->
                    "%s %s(%s) { %s }".formatted(javaOutput(type), name, args.stream().map(Main::javaOutput).collect(Collectors.joining(", ")), declarations.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")) + commands.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")));

            case ProcedureDeclarationNode(String name, List<Node> args, List<Node> declarations, List<Node> commands, _) ->
                    "void %s(%s) { %s }".formatted(name, args.stream().map(Main::javaOutput).collect(Collectors.joining(", ")), declarations.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")) + commands.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")));

            case DivNode(Node left, Node right, _) -> "%s / %s".formatted(javaOutput(left), javaOutput(right));
            case MulNode(Node left, Node right, _) -> "%s * %s".formatted(javaOutput(left), javaOutput(right));
            case AddNode(Node left, Node right, _) -> "%s + %s".formatted(javaOutput(left), javaOutput(right));
            case SubNode(Node left, Node right, _) -> "%s - %s".formatted(javaOutput(left), javaOutput(right));
            case ModNode(Node left, Node right, _) -> "%s %% %s".formatted(javaOutput(left), javaOutput(right));
            case AndNode(Node left, Node right, _) -> "%s && %s".formatted(javaOutput(left), javaOutput(right));
            case OrNode(Node left, Node right, _) -> "%s || %s".formatted(javaOutput(left), javaOutput(right));
            case NotNode(Node expr, _) -> "!%s".formatted(javaOutput(expr));
            case EqNode(Node left, Node right, _) -> "%s == %s".formatted(javaOutput(left), javaOutput(right));
            case NeNode(Node left, Node right, _) -> "%s != %s".formatted(javaOutput(left), javaOutput(right));
            case PowNode(Node left, Node right, _) -> "Math.pow(%s, %s)".formatted(javaOutput(left), javaOutput(right));
            case GtNode(Node left, Node right, _) -> "%s > %s".formatted(javaOutput(left), javaOutput(right));
            case GeNode(Node left, Node right, _) -> "%s >= %s".formatted(javaOutput(left), javaOutput(right));
            case LtNode(Node left, Node right, _) -> "%s < %s".formatted(javaOutput(left), javaOutput(right));
            case LeNode(Node left, Node right, _) -> "%s <= %s".formatted(javaOutput(left), javaOutput(right));
            case NegNode(Node expr, _) -> "-%s".formatted(javaOutput(expr));

            default -> "// to be implemented (" + node + ")";
        };
    }

    static class BaseErrorListener extends org.antlr.v4.runtime.BaseErrorListener {
        @Override
        public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line, int charPositionInLine, String msg, RecognitionException e) {
            throw new RuntimeException("line " + line + ":" + charPositionInLine + " " + msg);
        }
    }

}
