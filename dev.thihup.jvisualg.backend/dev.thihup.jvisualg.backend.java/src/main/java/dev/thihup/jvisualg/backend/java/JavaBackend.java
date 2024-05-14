package dev.thihup.jvisualg.backend.java;

import dev.thihup.jvisualg.frontend.node.Node;

import java.util.List;
import java.util.stream.Collectors;

public class JavaBackend {
    static String javaOutput(Node node) {
        return switch (node) {
            case null -> "// to be implemented (null?)";
            case Node.AlgoritimoNode(
                    Node.StringLiteralNode text, Node declarations, Node commands, _
            ) -> """
                    // %s
                    %s
                    void main() {
                        %s
                    }
                    """.formatted(text.value(), javaOutput(declarations), javaOutput(commands));


            case Node.CompundNode(
                    var nodes, _
            ) -> nodes.stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t"));

            case Node.VariableDeclarationNode(Node.IdNode name, Node.ArrayTypeNode type, _) ->
                    "%s%s %s = new %s%s;".formatted(javaOutput(type.type()), "[]".repeat(type.sizes().nodes().size()), name.id(), javaOutput(type.type()), type.sizes().nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining("][", "[", "]")));

            case Node.VariableDeclarationNode(Node.IdNode name, Node type, _) -> "%s %s;".formatted(javaOutput(type), name.id());

            case Node.ConstantNode(Node.IdNode name, Node.IntLiteralNode(int value, _), _) ->
                    "private static final int %s = %s;".formatted(name.id(), value);
            case Node.ConstantNode(Node.IdNode name, Node.RealLiteralNode(double value, _), _) ->
                    "private static final double %s = %s;".formatted(name.id(), value);
            case Node.ConstantNode(Node.IdNode name, Node.StringLiteralNode(String value, _), _) ->
                    "private static final String %s = %s;".formatted(name.id(), value);
            case Node.ConstantNode(Node.IdNode name, Node.BooleanLiteralNode(boolean value, _), _) ->
                    "private static final boolean %s = %s;".formatted(name.id(), value);


            case Node.TypeNode(Node.StringLiteralNode(String type, _), _) -> switch (type.toLowerCase()) {
                case "inteiro" -> "int";
                case "real" -> "double";
                case "caracter", "caractere", "literal" -> "String";
                case "logico" -> "boolean";
                default -> type;
            };

            case Node.InterrompaCommandNode(_) -> "break;";

            case Node.ReturnNode(Node expr, _) -> "return %s;".formatted(javaOutput(expr));

            case Node.DosNode(_) -> "";

            case Node.AssignmentNode(Node idOrArray, Node expr, _) ->
                    "%s = %s;".formatted(javaOutput(idOrArray), javaOutput(expr));

            case Node.IncrementNode(Node expr, Node value, _) ->
                    "%s += %s".formatted(javaOutput(expr), javaOutput(value));


            case Node.ForCommandNode(Node.IdNode identifier, Node startValue, Node endValue, Node step, var commands, _) -> {
                yield """
                        for (%s = %s; %s <= %s; %s += %s) {
                            %s
                        }
                        """.formatted(javaOutput(identifier), javaOutput(startValue), javaOutput(identifier), javaOutput(endValue), javaOutput(identifier), javaOutput(step), commands.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining()));
            }

            case Node.WhileCommandNode(Node condition, var commands, boolean atTheEnd, _) -> {
                if (atTheEnd) {
                    yield """
                            do {
                                %s
                            } while (%s);
                            """.formatted(commands.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")), javaOutput(condition));
                } else {
                    yield """
                            while (%s) {
                                %s
                            }
                            """.formatted(javaOutput(condition), commands.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")));
                }
            }

            case Node.WriteCommandNode(boolean newLine, var writeList, _) -> {
                if (newLine) {
                    yield "System.out.println(%s);".formatted(writeList.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining(" + ")));
                } else {
                    yield "System.out.print(%s);".formatted(writeList.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining(" + ")));
                }
            }

            case Node.WriteItemNode(Node expr, Node spaces, Node precision, _) -> {
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
                    Node condition, var thenCommands, var elseCommands, _
            ) -> {
                if (elseCommands.nodes().isEmpty()) {
                    yield """
                            if (%s) {
                                %s
                            }
                            """.formatted(javaOutput(condition), thenCommands.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")));
                } else {
                    yield """
                            if (%s) {
                                %s
                            } else {
                                %s
                            }
                            """.formatted(javaOutput(condition), thenCommands.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")), elseCommands.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")));
                }
            }

            case Node.RegistroDeclarationNode(Node.IdNode name, var variableDeclarationContexts, _) -> """
                    class %s {
                        %s
                    }
                    """.formatted(name, variableDeclarationContexts.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")));

            case Node.IntLiteralNode(int value, _) -> String.valueOf(value);

            case Node.RealLiteralNode(double value, _) -> String.valueOf(value);

            case Node.StringLiteralNode(String value, _) -> value;

            case Node.BooleanLiteralNode(boolean value, _) -> String.valueOf(value);

            case Node.IdNode(String id, _) -> id;

            case Node.ArrayAccessNode(Node.IdNode id, var indexes, _) ->
                    "%s[%s-1]".formatted(javaOutput(id), indexes.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining("][")));


            case Node.FunctionCallNode(Node.IdNode name, var args, _) ->
                    "%s(%s)".formatted(name.id(), args.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining(", ")));

            case Node.ProcedureCallNode(Node.IdNode name, var args, _) ->
                    "%s(%s)".formatted(name.id(), args.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining(", ")));

            case Node.FunctionDeclarationNode(
                    Node.IdNode name, Node type, var args, var references, var declarations, var commands, _
            ) ->
                    "%s %s(%s) { %s }".formatted(javaOutput(type), name, args.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining(", ")), declarations.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")) + commands.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")));

            case Node.ProcedureDeclarationNode(
                    Node.IdNode name, var args, var references, var declarations, var commands, _
            ) ->
                    "void %s(%s) { %s }".formatted(name, args.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining(", ")), declarations.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")) + commands.nodes().stream().map(JavaBackend::javaOutput).collect(Collectors.joining("\n\t")));

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
