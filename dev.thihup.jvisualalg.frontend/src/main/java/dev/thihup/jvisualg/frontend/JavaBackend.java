package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.frontend.node.Location;
import dev.thihup.jvisualg.frontend.node.Node;

import java.util.List;
import java.util.stream.Collectors;

public class JavaBackend {
    static String javaOutput(Node node) {
        return switch (node) {
            case null -> "// to be implemented (null?)";
            case Node.AlgoritimoNode(
                    String text, Node declarations, Node commands, _
            ) -> """
                    // %s
                    %s
                    void main() {
                        %s
                    }
                    """.formatted(text, javaOutput(declarations), javaOutput(commands));

            case Node.DeclarationsNode(
                    List<Node> variableDeclarationContexts, List<Node> registroDeclarationContexts,
                    List<Node> subprogramDeclarationContexts, List<Node> constantsDeclarationContexts,
                    _, _
            ) -> """
                    %s
                    %s
                    %s
                    %s
                    """.formatted(constantsDeclarationContexts.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")),
                    variableDeclarationContexts.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")),
                    registroDeclarationContexts.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")),
                    subprogramDeclarationContexts.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")));


            case Node.CompundNode(
                    List<? extends Node> nodes, _
            ) -> nodes.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t"));

            case Node.VariableDeclarationNode(String name, Node.ArrayTypeNode type, _) ->
                    "%s%s %s = new %s%s;".formatted(javaOutput(type.type()), "[]".repeat((int) type.dimensions()), name, javaOutput(type.type()), type.sizes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining("][", "[", "]")));

            case Node.VariableDeclarationNode(String name, Node type, _) -> "%s %s;".formatted(javaOutput(type), name);

            case Node.ConstantsDeclarationNode(List<Node.ConstantNode> constants, _) ->
                    constants.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t"));

            case Node.ConstantNode(String name, Node.IntLiteralNode(int value, _), _) ->
                    "private static final int %s = %s;".formatted(name, value);
            case Node.ConstantNode(String name, Node.RealLiteralNode(double value, _), _) ->
                    "private static final double %s = %s;".formatted(name, value);
            case Node.ConstantNode(String name, Node.StringLiteralNode(String value, _), _) ->
                    "private static final String %s = %s;".formatted(name, value);
            case Node.ConstantNode(String name, Node.BooleanLiteralNode(boolean value, _), _) ->
                    "private static final boolean %s = %s;".formatted(name, value);


            case Node.TypeNode(String type, _) -> switch (type.toLowerCase()) {
                case "inteiro" -> "int";
                case "real" -> "double";
                case "caracter", "caractere", "literal" -> "String";
                case "logico" -> "boolean";
                default -> type;
            };

            case Node.CommandsNode(List<Node> commands, _) ->
                    commands.stream().map(JavaBackend::javaOutput).map("\t"::concat).collect(Collectors.joining("\n\t"));

            case Node.CommandNode(Node command, _) -> javaOutput(command);

            case Node.InterrompaCommandNode(_) -> "break;";

            case Node.ReturnNode(Node expr, _) -> "return %s;".formatted(javaOutput(expr));

            case Node.DosNode(_) -> "";

            case Node.AssignmentNode(Node idOrArray, Node expr, _) ->
                    "%s = %s;".formatted(javaOutput(idOrArray), javaOutput(expr));

            case Node.IncrementNode(Node expr, Node value, _) ->
                    "%s += %s".formatted(javaOutput(expr), javaOutput(value));

//            case ForCommandNode(Node start, Node test, Node end, List<Node> commands, _) -> {
//                String s = start != null ? "int " + javaOutput(start) : ";";
//                yield """
//                    for (%s %s; %s) {
//                        %s
//                    }
//                    """.formatted(s, javaOutput(test), javaOutput(end), commands.stream().map(Main::javaOutput).collect(Collectors.joining("\n\t")));
//            }

            case Node.WhileCommandNode(Node condition, List<Node> commands, boolean atTheEnd, _) -> {
                if (atTheEnd) {
                    yield """
                            do {
                                %s
                            } while (%s);
                            """.formatted(commands.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")), javaOutput(condition));
                } else {
                    yield """
                            while (%s) {
                                %s
                            }
                            """.formatted(javaOutput(condition), commands.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")));
                }
            }

            case Node.WriteCommandNode(boolean newLine, List<Node> writeList, _) -> {
                if (newLine) {
                    yield "System.out.println(%s);".formatted(writeList.stream().map(JavaBackend::javaOutput).collect(Collectors.joining(" + ")));
                } else {
                    yield "System.out.print(%s);".formatted(writeList.stream().map(JavaBackend::javaOutput).collect(Collectors.joining(" + ")));
                }
            }

            case Node.WriteItemNode(Node expr, Integer spaces, Integer precision, _) -> {
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

            case Node.ConditionalCommandNode(
                    Node condition, List<Node> thenCommands, List<Node> elseCommands, Location _
            ) -> {
                if (elseCommands.isEmpty()) {
                    yield """
                            if (%s) {
                                %s
                            }
                            """.formatted(javaOutput(condition), thenCommands.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")));
                } else {
                    yield """
                            if (%s) {
                                %s
                            } else {
                                %s
                            }
                            """.formatted(javaOutput(condition), thenCommands.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")), elseCommands.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")));
                }
            }

            case Node.RegistroDeclarationNode(String name, List<Node> variableDeclarationContexts, _) -> """
                    class %s {
                        %s
                    }
                    """.formatted(name, variableDeclarationContexts.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")));

            case Node.IntLiteralNode(int value, _) -> String.valueOf(value);

            case Node.RealLiteralNode(double value, _) -> String.valueOf(value);

            case Node.StringLiteralNode(String value, _) -> value;

            case Node.BooleanLiteralNode(boolean value, _) -> String.valueOf(value);

            case Node.IdNode(String id, _) -> id;

            case Node.ArrayAccessNode(Node.IdNode id, List<Node> indexes, _) ->
                    "%s[%s-1]".formatted(javaOutput(id), indexes.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("][")));


            case Node.FunctionCallNode(String name, List<Node> args, Location location) ->
                    "%s(%s)".formatted(name, args.stream().map(JavaBackend::javaOutput).collect(Collectors.joining(", ")));

            case Node.ProcedureCallNode(String name, List<Node> args, Location location) ->
                    "%s(%s)".formatted(name, args.stream().map(JavaBackend::javaOutput).collect(Collectors.joining(", ")));

            case Node.FunctionDeclarationNode(
                    String name, Node type, List<Node> args, List<Node> references, List<Node> declarations, List<Node> commands, _
            ) ->
                    "%s %s(%s) { %s }".formatted(javaOutput(type), name, args.stream().map(JavaBackend::javaOutput).collect(Collectors.joining(", ")), declarations.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")) + commands.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")));

            case Node.ProcedureDeclarationNode(
                    String name, List<Node> args, List<Node> references, List<Node> declarations, List<Node> commands, _
            ) ->
                    "void %s(%s) { %s }".formatted(name, args.stream().map(JavaBackend::javaOutput).collect(Collectors.joining(", ")), declarations.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")) + commands.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")));

            case Node.DivNode(Node left, Node right, _) -> "%s / %s".formatted(javaOutput(left), javaOutput(right));
            case Node.MulNode(Node left, Node right, _) -> "%s * %s".formatted(javaOutput(left), javaOutput(right));
            case Node.AddNode(Node left, Node right, _) -> "%s + %s".formatted(javaOutput(left), javaOutput(right));
            case Node.SubNode(Node left, Node right, _) -> "%s - %s".formatted(javaOutput(left), javaOutput(right));
            case Node.ModNode(Node left, Node right, _) -> "%s %% %s".formatted(javaOutput(left), javaOutput(right));
            case Node.AndNode(Node left, Node right, _) -> "%s && %s".formatted(javaOutput(left), javaOutput(right));
            case Node.OrNode(Node left, Node right, _) -> "%s || %s".formatted(javaOutput(left), javaOutput(right));
            case Node.NotNode(Node expr, _) -> "!%s".formatted(javaOutput(expr));
            case Node.EqNode(Node left, Node right, _) -> "%s == %s".formatted(javaOutput(left), javaOutput(right));
            case Node.NeNode(Node left, Node right, _) -> "%s != %s".formatted(javaOutput(left), javaOutput(right));
            case Node.PowNode(Node left, Node right, _) ->
                    "Math.pow(%s, %s)".formatted(javaOutput(left), javaOutput(right));
            case Node.GtNode(Node left, Node right, _) -> "%s > %s".formatted(javaOutput(left), javaOutput(right));
            case Node.GeNode(Node left, Node right, _) -> "%s >= %s".formatted(javaOutput(left), javaOutput(right));
            case Node.LtNode(Node left, Node right, _) -> "%s < %s".formatted(javaOutput(left), javaOutput(right));
            case Node.LeNode(Node left, Node right, _) -> "%s <= %s".formatted(javaOutput(left), javaOutput(right));
            case Node.NegNode(Node expr, _) -> "-%s".formatted(javaOutput(expr));

            default -> "// to be implemented (" + node + ")";
        };
    }
}
