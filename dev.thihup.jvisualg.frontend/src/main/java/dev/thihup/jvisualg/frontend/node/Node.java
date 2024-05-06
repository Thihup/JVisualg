package dev.thihup.jvisualg.frontend.node;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

public sealed interface Node {
    Optional<Location> location();

    enum EmptyNode implements Node {
        INSTANCE;

        @Override
        public Optional<Location> location() {
            return Optional.empty();
        }
    }

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
            case ArrayTypeNode(var type, CompundNode(var nodes, _), _) -> {
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
            case EmptyNode _ -> Stream.of();
        };
        return childrenNode.mapMulti((Node element, Consumer<Node>  downstream) -> {
            downstream.accept(element);
            if (element != null)
                element.visitChildren().forEach(downstream);
        });
    }

    record AlgoritimoNode(StringLiteralNode text, Node declarations, Node commands, Optional<Location> location) implements Node {
        public AlgoritimoNode {
            Objects.requireNonNull(text);
            Objects.requireNonNull(declarations);
            Objects.requireNonNull(commands);
            Objects.requireNonNull(location);
        }
    }

    sealed interface SubprogramDeclarationNode extends Node {
    }

    record FunctionDeclarationNode(IdNode name, Node returnType, CompundNode parameters, CompundNode references, CompundNode declarations,CompundNode commands, Optional<Location> location) implements SubprogramDeclarationNode {
        public FunctionDeclarationNode {
            Objects.requireNonNull(name);
            Objects.requireNonNull(returnType);
            Objects.requireNonNull(parameters);
            Objects.requireNonNull(references);
            Objects.requireNonNull(declarations);
            Objects.requireNonNull(commands);
            Objects.requireNonNull(location);
        }
    }

    record ProcedureDeclarationNode(IdNode name, CompundNode parameters, CompundNode references, CompundNode declarations, CompundNode commands, Optional<Location> location) implements SubprogramDeclarationNode {
        public ProcedureDeclarationNode {
            Objects.requireNonNull(name);
            Objects.requireNonNull(parameters);
            Objects.requireNonNull(references);
            Objects.requireNonNull(declarations);
            Objects.requireNonNull(commands);
        }
    }

    record RegistroDeclarationNode(IdNode name, CompundNode variableDeclarationContexts, Optional<Location> location) implements Node {
        public RegistroDeclarationNode {
            Objects.requireNonNull(name);
            Objects.requireNonNull(variableDeclarationContexts);
            Objects.requireNonNull(location);
        }
    }

    record VariableDeclarationNode(IdNode name, Node type, Optional<Location> location) implements Node {
        public VariableDeclarationNode {
            Objects.requireNonNull(name);
            Objects.requireNonNull(type);
            Objects.requireNonNull(location);
        }
    }

    record CompundNode(List<Node> nodes, Optional<Location> location) implements Node {
        public static final CompundNode EMPTY = new CompundNode(List.of(), Optional.empty());

        public CompundNode {
            Objects.requireNonNull(nodes);
            Objects.requireNonNull(location);
        }
    }

    record ConstantNode(IdNode name, Node value, Optional<Location> location) implements Node {
        public ConstantNode {
            Objects.requireNonNull(name);
            Objects.requireNonNull(value);
            Objects.requireNonNull(location);
        }
    }

    sealed interface LiteralNode extends Node {
    }

    record BooleanLiteralNode(boolean value, Optional<Location> location) implements LiteralNode {
        public BooleanLiteralNode {
            Objects.requireNonNull(location);
        }
    }

    record IntLiteralNode(int value, Optional<Location> location) implements LiteralNode {
        public IntLiteralNode {
            Objects.requireNonNull(location);
        }
    }

    record RealLiteralNode(double value, Optional<Location> location) implements LiteralNode {
        public RealLiteralNode {
            Objects.requireNonNull(location);
        }
    }

    record StringLiteralNode(String value, Optional<Location> location) implements LiteralNode {
        public StringLiteralNode {
            Objects.requireNonNull(location);
        }
    }
    record TypeNode(Node type, Optional<Location> location) implements Node {
        public TypeNode {
            Objects.requireNonNull(type);
            Objects.requireNonNull(location);
        }
    }

    sealed interface CommandNode extends Node {}

    record InterrompaCommandNode(Optional<Location> location) implements CommandNode {
        public InterrompaCommandNode {
            Objects.requireNonNull(location);
        }
    }

    record ReturnNode(Node expr, Optional<Location> location) implements CommandNode {
        public ReturnNode {
            Objects.requireNonNull(expr);
            Objects.requireNonNull(location);
        }
    }

    record DosNode(Optional<Location> location) implements Node {
        public DosNode {
            Objects.requireNonNull(location);
        }
    }

    record IncrementNode(Node expr, Node value, Optional<Location> location) implements CommandNode {
        public IncrementNode {
            Objects.requireNonNull(expr);
            Objects.requireNonNull(value);
            Objects.requireNonNull(location);
        }
    }

    record AssignmentNode(Node idOrArray, Node expr, Optional<Location> location) implements CommandNode {
        public AssignmentNode {
            Objects.requireNonNull(idOrArray);
            Objects.requireNonNull(expr);
            Objects.requireNonNull(location);
        }
    }

    record IdNode(String id, Optional<Location> location) implements Node {
        public IdNode {
            Objects.requireNonNull(id);
            Objects.requireNonNull(location);
        }
    }

    record ArrayAccessNode(Node node, CompundNode indexes, Optional<Location> location) implements CommandNode {
        public ArrayAccessNode {
            Objects.requireNonNull(node);
            Objects.requireNonNull(indexes);
            Objects.requireNonNull(location);
        }
    }

    record MemberAccessNode(Node node, Node member, Optional<Location> location) implements CommandNode {
        public MemberAccessNode {
            Objects.requireNonNull(node);
            Objects.requireNonNull(member);
            Objects.requireNonNull(location);
        }
    }

    record ReadCommandNode(CompundNode exprList, Optional<Location> location) implements CommandNode {
        public ReadCommandNode {
            Objects.requireNonNull(exprList);
            Objects.requireNonNull(location);
        }
    }

    record WriteCommandNode(boolean newLine, CompundNode writeList, Optional<Location> location) implements CommandNode {
        public WriteCommandNode {
            Objects.requireNonNull(writeList);
            Objects.requireNonNull(location);
        }
    }

    record WriteItemNode(Node expr, Node spaces, Node precision, Optional<Location> location) implements CommandNode {
        public WriteItemNode {
            Objects.requireNonNull(expr);
            Objects.requireNonNull(spaces);
            Objects.requireNonNull(precision);
            Objects.requireNonNull(location);
        }
    }

    record ConditionalCommandNode(Node expr, CompundNode commands, CompundNode elseCommands, Optional<Location> location) implements CommandNode {
        public ConditionalCommandNode {
            Objects.requireNonNull(expr);
            Objects.requireNonNull(commands);
            Objects.requireNonNull(elseCommands);
            Objects.requireNonNull(location);
        }
    }

    record RangeNode(Node start, Node end, Optional<Location> location) implements Node {
        public RangeNode {
            Objects.requireNonNull(start);
            Objects.requireNonNull(end);
            Objects.requireNonNull(location);
        }
    }

    record ChooseCommandNode(Node expr, CompundNode cases, Node defaultCase, Optional<Location> location) implements CommandNode {
        public ChooseCommandNode {
            Objects.requireNonNull(expr);
            Objects.requireNonNull(cases);
            Objects.requireNonNull(defaultCase);
            Objects.requireNonNull(location);
        }
    }

    record ChooseCaseNode(Node value, CompundNode commands, Optional<Location> location) implements CommandNode {
        public ChooseCaseNode {
            Objects.requireNonNull(value);
            Objects.requireNonNull(commands);
            Objects.requireNonNull(location);
        }
    }

    record WhileCommandNode(Node test, CompundNode commands, boolean conditionAtEnd, Optional<Location> location) implements CommandNode {
        public WhileCommandNode {
            Objects.requireNonNull(test);
            Objects.requireNonNull(commands);
            Objects.requireNonNull(location);
        }
    }

    record ForCommandNode(IdNode identifier, Node startValue, Node endValue, Node step, CompundNode commands, Optional<Location> location) implements CommandNode {
        public ForCommandNode {
            Objects.requireNonNull(identifier);
            Objects.requireNonNull(startValue);
            Objects.requireNonNull(endValue);
            Objects.requireNonNull(step);
            Objects.requireNonNull(commands);
            Objects.requireNonNull(location);
        }
    }

    record ProcedureCallNode(IdNode name, CompundNode args, Optional<Location> location) implements CommandNode {
        public ProcedureCallNode {
            Objects.requireNonNull(name);
            Objects.requireNonNull(args);
            Objects.requireNonNull(location);
        }
    }

    record FunctionCallNode(IdNode name, CompundNode args, Optional<Location> location) implements CommandNode {
        public FunctionCallNode {
            Objects.requireNonNull(name);
            Objects.requireNonNull(args);
            Objects.requireNonNull(location);
        }
    }

    sealed interface BinaryNode extends CommandNode {
        Node left();

        Node right();
    }

    record AddNode(Node left, Node right, Optional<Location> location) implements BinaryNode {
        public AddNode {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    record SubNode(Node left, Node right, Optional<Location> location) implements BinaryNode {
        public SubNode {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    record MulNode(Node left, Node right, Optional<Location> location) implements BinaryNode {
        public MulNode {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    record DivNode(Node left, Node right, Optional<Location> location) implements BinaryNode {
        public DivNode {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    record ModNode(Node left, Node right, Optional<Location> location) implements BinaryNode {
        public ModNode {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    record PowNode(Node left, Node right, Optional<Location> location) implements BinaryNode {
        public PowNode {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    sealed interface BooleanNode extends CommandNode {}

    record AndNode(Node left, Node right, Optional<Location> location) implements BooleanNode, BinaryNode {
        public AndNode {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    record OrNode(Node left, Node right, Optional<Location> location) implements BooleanNode, BinaryNode {
        public OrNode {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    record NotNode(Node expr, Optional<Location> location) implements BooleanNode {
        public NotNode {
            Objects.requireNonNull(expr);
            Objects.requireNonNull(location);
        }
    }

    sealed interface RelationalNode extends BinaryNode {}

    record EqNode(Node left, Node right, Optional<Location> location) implements RelationalNode {
        public EqNode {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    record NeNode(Node left, Node right, Optional<Location> location) implements RelationalNode {
        public NeNode {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    record LtNode(Node left, Node right, Optional<Location> location) implements RelationalNode {
        public LtNode {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    record LeNode(Node left, Node right, Optional<Location> location) implements RelationalNode {
        public LeNode {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    record GtNode(Node left, Node right, Optional<Location> location) implements RelationalNode {
        public GtNode {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    record GeNode(Node left, Node right, Optional<Location> location) implements RelationalNode {
        public GeNode {
            Objects.requireNonNull(left);
            Objects.requireNonNull(right);
            Objects.requireNonNull(location);
        }
    }

    record NegNode(Node expr, Optional<Location> location) implements CommandNode {
        public NegNode {
            Objects.requireNonNull(expr);
            Objects.requireNonNull(location);
        }
    }

    record PosNode(Node expr, Optional<Location> location) implements CommandNode {
        public PosNode {
            Objects.requireNonNull(expr);
            Objects.requireNonNull(location);
        }
    }


    record ArquivoCommandNode(Node name, Optional<Location> location) implements CommandNode {
        public ArquivoCommandNode {
            Objects.requireNonNull(name);
            Objects.requireNonNull(location);
        }
    }

    record AleatorioCommandNode(boolean on, List<Integer> args, Optional<Location> location) implements CommandNode {
        public AleatorioCommandNode {
            Objects.requireNonNull(args);
            Objects.requireNonNull(location);
        }
    }

    record TimerCommandNode(boolean on, int value, Optional<Location> location) implements CommandNode {
        public TimerCommandNode {
            Objects.requireNonNull(location);
        }
    }

    record PausaCommandNode(Optional<Location> location) implements CommandNode {
        public PausaCommandNode {
            Objects.requireNonNull(location);
        }
    }

    record DebugCommandNode(Node expr, Optional<Location> location) implements CommandNode {
        public DebugCommandNode {
            Objects.requireNonNull(expr);
            Objects.requireNonNull(location);
        }
    }

    record EcoCommandNode(boolean on, Optional<Location> location) implements CommandNode {
        public EcoCommandNode {
            Objects.requireNonNull(location);
        }
    }

    record CronometroCommandNode(boolean on, Optional<Location> location) implements CommandNode {
        public CronometroCommandNode {
            Objects.requireNonNull(location);
        }
    }

    record LimpatelaCommandNode(Optional<Location> location) implements CommandNode {
        public LimpatelaCommandNode {
            Objects.requireNonNull(location);
        }
    }

    record ArrayTypeNode(TypeNode type, CompundNode sizes, Optional<Location> location) implements Node {
        public ArrayTypeNode {
            Objects.requireNonNull(type);
            Objects.requireNonNull(sizes);
            Objects.requireNonNull(location);
        }
    }
}
