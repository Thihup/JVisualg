package dev.thihup.jvisualg.frontend.node;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

public sealed interface Node {
    Location location();

    default Stream<Node> visitChildren() {
        Stream<Node> childrenNode = switch (this) {
            case AlgoritimoNode(var literalNode, var declarations, var commands, _) ->
                    Stream.of(literalNode, declarations, commands);
            case FunctionDeclarationNode(
                    var name, var returnType, var parameters, var references, var declarations, var commands, _
            ) -> {
                Stream<Node> nodeStream = Stream.of(name, returnType);
                Stream<Node> parametersStream = parameters.nodes().stream();
                Stream<Node> referencesStream = references.nodes().stream();
                Stream<Node> declarationsStream = declarations.nodes().stream();
                Stream<Node> commandsStream = commands.nodes().stream();
                yield Stream.of(nodeStream, parametersStream, referencesStream, declarationsStream, commandsStream).flatMap(s -> s);
            }
            case ProcedureDeclarationNode(
                    var name, var parameters, var references, var declarations, var commands, _
            ) -> {
                Stream<Node> nodeStream = Stream.of(name);
                Stream<Node> parametersStream = parameters.nodes().stream();
                Stream<Node> referencesStream = references.nodes().stream();
                Stream<Node> declarationsStream = declarations.nodes().stream();
                Stream<Node> commandsStream = commands.nodes().stream();
                yield Stream.of(nodeStream, parametersStream, referencesStream, declarationsStream, commandsStream).flatMap(s -> s);
            }
            case RegistroDeclarationNode(var name, CompundNode(var nodes, _), _) -> {
                Stream<Node> nodeStream = Stream.of(name);
                Stream<Node> nodesStream = nodes.stream();
                yield Stream.concat(nodeStream, nodesStream);
            }
            case VariableDeclarationNode(var name, var type, _) -> Stream.of(name, type);
            case CompundNode(var nodes, _) -> nodes.stream();
            case ConstantNode(var name, var value, _) -> Stream.of(name, value);
            case BooleanLiteralNode _, IntLiteralNode _, RealLiteralNode _, StringLiteralNode _, TypeNode _ ->
                    Stream.of();

            case BinaryNode binaryNode -> Stream.of(binaryNode.left(), binaryNode.right());

            case NegNode(var expr, _) -> Stream.of(expr);
            case PosNode(var expr, _) -> Stream.of(expr);

            case ArrayAccessNode(var accessor, CompundNode(var nodes, _), _) -> {
                Stream<Node> arrayNode = Stream.of(accessor);
                Stream<Node> indexesStream = nodes.stream();
                yield Stream.concat(arrayNode, indexesStream);
            }
            case MemberAccessNode(var root, var member, _) -> Stream.of(root, member);
            case ReadCommandNode(CompundNode(var nodes, _), _) -> nodes.stream();
            case WriteCommandNode(_, CompundNode(var nodes, _), _) -> nodes.stream();
            case WriteItemNode(var expr, _, _, _) -> Stream.of(expr);
            case ConditionalCommandNode(var expr, CompundNode(var nodes, _), CompundNode(var elseNodes, _), _) -> {
                Stream<Node> exprStream = Stream.of(expr);
                Stream<Node> commands = nodes.stream();
                Stream<Node> elseCommands = elseNodes.stream();
                yield Stream.of(exprStream, commands, elseCommands).flatMap(s -> s);
            }
            case RangeNode(var start, var end, _) -> Stream.of(start, end);
            case ChooseCommandNode(var expr, CompundNode(var nodes, _), var defaultCase, _) -> {
                Stream<Node> exprStream = Stream.of(expr);
                Stream<Node> cases = nodes.stream();
                Stream<Node> defaultCaseStream = Stream.of(defaultCase);
                yield Stream.of(exprStream, cases, defaultCaseStream).flatMap(s -> s);
            }
            case ChooseCaseNode(var value, CompundNode(var nodes, _), _) -> {
                Stream<Node> valueStream = Stream.of(value);
                Stream<Node> commandsStream = nodes.stream();
                yield Stream.concat(valueStream, commandsStream);
            }
            case WhileCommandNode(var test, CompundNode(var nodes, _), _, _) -> {
                Stream<Node> testStream = Stream.of(test);
                Stream<Node> commandsStream = nodes.stream();
                yield Stream.concat(testStream, commandsStream);
            }
            case ForCommandNode(
                    var identifier, var startValue, var endValue, var step, CompundNode(var nodes, _), _
            ) -> {
                Stream<Node> header = Stream.of(identifier, startValue, endValue, step);
                Stream<Node> commands = nodes.stream();
                yield Stream.concat(header, commands);
            }
            case ProcedureCallNode(var name, CompundNode(var nodes, _), _) -> {
                Stream<Node> nameStream = Stream.of(name);
                Stream<Node> argsStream = nodes.stream();
                yield Stream.concat(nameStream, argsStream);
            }
            case FunctionCallNode(var name, CompundNode(var nodes, _), _) -> {
                Stream<Node> nameStream = Stream.of(name);
                Stream<Node> argsStream = nodes.stream();
                yield Stream.concat(nameStream, argsStream);
            }
            case DebugCommandNode debugCommandNode -> Stream.of(debugCommandNode.expr());
            case ArquivoCommandNode _, AleatorioCommandNode _, TimerCommandNode _, PausaCommandNode _, EcoCommandNode _,
                 CronometroCommandNode _, LimpatelaCommandNode _ -> Stream.of();
            case ArrayTypeNode(var type, _, CompundNode(var nodes, _), _) -> {
                Stream<Node> typeStream = Stream.of(type);
                Stream<Node> sizes = nodes.stream();
                yield Stream.concat(typeStream, sizes);
            }

            case AssignmentNode assignmentNode ->
                    Stream.of(assignmentNode.idOrArray(), assignmentNode.expr());
            case DosNode _, IdNode _ -> Stream.of();
            case IncrementNode(var expr, var value, _) -> Stream.of(expr, value);
            case InterrompaCommandNode _ -> Stream.of();
            case NotNode(var expr, _) -> Stream.of(expr);
            case ReturnNode(var expr, _) -> Stream.of(expr);
        };
        return childrenNode.mapMulti((Node element, Consumer<Node>  downstream) -> {
            downstream.accept(element);
            if (element != null)
                element.visitChildren().forEach(downstream);
        });
    }

    record AlgoritimoNode(StringLiteralNode text, Node declarations, Node commands, Location location) implements Node {
    }

    record FunctionDeclarationNode(IdNode name, Node returnType, CompundNode parameters, CompundNode references, CompundNode declarations,CompundNode commands, Location location) implements Node {
    }

    record ProcedureDeclarationNode(IdNode name, CompundNode parameters, CompundNode references, CompundNode declarations, CompundNode commands, Location location) implements Node {
    }

    record RegistroDeclarationNode(IdNode name, CompundNode variableDeclarationContexts, Location location) implements Node {
    }

    record VariableDeclarationNode(IdNode name, Node type, Location location) implements Node {
    }

    record CompundNode(List<Node> nodes, Location location) implements Node {
        public static final CompundNode EMPTY = new CompundNode(List.of(), null);
    }

    record ConstantNode(IdNode name, Node value, Location location) implements Node {
    }

    sealed interface LiteralNode extends Node {
    }

    record BooleanLiteralNode(boolean value, Location location) implements LiteralNode {
    }

    record IntLiteralNode(int value, Location location) implements LiteralNode {
    }

    record RealLiteralNode(double value, Location location) implements LiteralNode {
    }

    record StringLiteralNode(String value, Location location) implements LiteralNode {
    }
    record TypeNode(String type, Location location) implements Node {
    }

    sealed interface CommandNode extends Node {}

    record InterrompaCommandNode(Location location) implements CommandNode {
    }

    record ReturnNode(Node expr, Location location) implements CommandNode {
    }

    record DosNode(Location location) implements Node {
    }

    record IncrementNode(Node expr, Node value, Location location) implements CommandNode {
    }

    record AssignmentNode(Node idOrArray, Node expr, Location location) implements CommandNode {
    }

    record IdNode(String id, Location location) implements Node {
    }

    record ArrayAccessNode(Node node, CompundNode indexes, Location location) implements CommandNode {
    }

    record MemberAccessNode(Node node, Node member, Location location) implements CommandNode {
    }

    record ReadCommandNode(CompundNode exprList, Location location) implements CommandNode {
    }

    record WriteCommandNode(boolean newLine, CompundNode writeList, Location location) implements CommandNode {
    }

    record WriteItemNode(Node expr, Integer spaces, Integer precision, Location location) implements CommandNode {
    }

    record ConditionalCommandNode(Node expr, CompundNode commands, CompundNode elseCommands, Location location) implements CommandNode {
    }

    record RangeNode(Node start, Node end, Location location) implements Node {
    }

    record ChooseCommandNode(Node expr, CompundNode cases, Node defaultCase, Location location) implements CommandNode {
    }

    record ChooseCaseNode(Node value, CompundNode commands, Location location) implements CommandNode {
    }

    record WhileCommandNode(Node test, CompundNode commands, boolean conditionAtEnd, Location location) implements CommandNode {
    }

    record ForCommandNode(IdNode identifier, Node startValue, Node endValue, Node step, CompundNode commands, Location location) implements CommandNode {
    }

    record ProcedureCallNode(IdNode name, CompundNode args, Location location) implements CommandNode {
    }

    record FunctionCallNode(IdNode name, CompundNode args, Location location) implements CommandNode {
    }

    sealed interface BinaryNode extends CommandNode {
        Node left();

        Node right();
    }

    record AddNode(Node left, Node right, Location location) implements BinaryNode {
    }

    record SubNode(Node left, Node right, Location location) implements BinaryNode {
    }

    record MulNode(Node left, Node right, Location location) implements BinaryNode {
    }

    record DivNode(Node left, Node right, Location location) implements BinaryNode {
    }

    record ModNode(Node left, Node right, Location location) implements BinaryNode {
    }

    record PowNode(Node left, Node right, Location location) implements BinaryNode {
    }

    sealed interface BooleanNode extends CommandNode {}

    record AndNode(Node left, Node right, Location location) implements BooleanNode, BinaryNode {
    }

    record OrNode(Node left, Node right, Location location) implements BooleanNode, BinaryNode {
    }

    record NotNode(Node expr, Location location) implements BooleanNode {
    }

    sealed interface RelationalNode extends BinaryNode {}

    record EqNode(Node left, Node right, Location location) implements RelationalNode {
    }

    record NeNode(Node left, Node right, Location location) implements RelationalNode {
    }

    record LtNode(Node left, Node right, Location location) implements RelationalNode {
    }

    record LeNode(Node left, Node right, Location location) implements RelationalNode {
    }

    record GtNode(Node left, Node right, Location location) implements RelationalNode {
    }

    record GeNode(Node left, Node right, Location location) implements RelationalNode {
    }

    record NegNode(Node expr, Location location) implements CommandNode {
    }

    record PosNode(Node expr, Location location) implements CommandNode {
    }


    record ArquivoCommandNode(String name, Location location) implements CommandNode {
    }

    record AleatorioCommandNode(boolean on, List<Integer> args, Location location) implements CommandNode {
    }

    record TimerCommandNode(boolean on, int value, Location location) implements CommandNode {
    }

    record PausaCommandNode(Location location) implements CommandNode {
    }

    record DebugCommandNode(Node expr, Location location) implements CommandNode {
    }

    record EcoCommandNode(boolean on, Location location) implements CommandNode {
    }

    record CronometroCommandNode(boolean on, Location location) implements CommandNode {
    }

    record LimpatelaCommandNode(Location location) implements CommandNode {
    }

    record ArrayTypeNode(TypeNode type, long dimensions, CompundNode sizes, Location location) implements Node {
    }
}
