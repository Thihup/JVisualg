package dev.thihup.jvisualg.frontend.node;

import java.util.List;

public sealed interface Node {
    Location location();

    record AlgoritimoNode(String text, Node declarations, Node commands, Location location) implements Node {
    }

    record DeclarationsNode(List<Node> variableDeclarationContexts, List<Node> registroDeclarationContexts, List<Node> subprogramDeclarationContexts, List<Node> constantsDeclarationContexts, List<Node> dosContexts, Location location) implements Node {
    }

    record FunctionDeclarationNode(String name, Node returnType, List<Node> parameters, List<Node> references, List<Node> declarations,List<Node> commands, Location location) implements Node {
    }

    record ProcedureDeclarationNode(String name, List<Node> parameters, List<Node> references, List<Node> declarations, List<Node> commands, Location location) implements Node {
    }

    record RegistroDeclarationNode(String name, List<Node> variableDeclarationContexts, Location location) implements Node {
    }

    record VariableDeclarationNode(String name, Node type, Location location) implements Node {
    }

    record CompundNode(List<? extends Node> nodes, Location location) implements Node {
    }

    record ConstantsDeclarationNode(List<ConstantNode> constants, Location location) implements Node {
    }

    record ConstantNode(String name, Node value, Location location) implements Node {
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

    record CommandsNode(List<Node> commands, Location location) implements Node {
    }

    record CommandNode(Node command, Location location) implements Node {
    }

    record InterrompaCommandNode(Location location) implements Node {
    }

    record ReturnNode(Node expr, Location location) implements Node {
    }

    record DosNode(Location location) implements Node {
    }

    record IncrementNode(Node expr, Node value, Location location) implements Node {
    }

    record AssignmentNode(Node idOrArray, Node expr, Location location) implements Node {
    }

    record IdNode(String id, Location location) implements Node {
    }

    record ArrayAccessNode(Node node, List<Node> indexes, Location location) implements Node {
    }

    record MemberAccessNode(Node node, Node member, Location location) implements Node {
    }

    record ReadCommandNode(List<Node> exprList, Location location) implements Node {
    }

    record WriteCommandNode(boolean newLine, List<Node> writeList, Location location) implements Node {
    }

    record WriteItemNode(Node expr, Integer spaces, Integer precision, Location location) implements Node {
    }

    record ConditionalCommandNode(Node expr, List<Node> commands, List<Node> elseCommands, Location location) implements Node {
    }

    record RangeNode(Node start, Node end, Location location) implements Node {
    }

    record ChooseCommandNode(Node expr, List<Node> cases, Node defaultCase, Location location) implements Node {
    }

    record ChooseCaseNode(Node value, List<Node> commands, Location location) implements Node {
    }

    record WhileCommandNode(Node test, List<Node> commands, boolean conditionAtEnd, Location location) implements Node {
    }

    record ForCommandNode(IdNode identifier, Node startValue, Node endValue, Node step, List<Node> commands, Location location) implements Node {
    }

    record ProcedureCallNode(String name, List<Node> args, Location location) implements Node {
    }

    record FunctionCallNode(String name, List<Node> args, Location location) implements Node {
    }

    sealed interface BinaryNode extends Node {
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

    sealed interface BooleanNode extends Node {}

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

    record NegNode(Node expr, Location location) implements Node {
    }

    record PosNode(Node expr, Location location) implements Node {
    }



    record ArquivoCommandNode(String name, Location location) implements Node {
    }

    record AleatorioCommandNode(boolean on, List<Integer> args, Location location) implements Node {
    }

    record TimerCommandNode(boolean on, int value, Location location) implements Node {
    }

    record PausaCommandNode(Location location) implements Node {
    }

    record DebugCommandNode(Node expr, Location location) implements Node {
    }

    record EcoCommandNode(boolean on, Location location) implements Node {
    }

    record CronometroCommandNode(boolean on, Location location) implements Node {
    }

    record LimpatelaCommandNode(Location location) implements Node {
    }

    record ArrayTypeNode(TypeNode type, long dimensions, List<Node> sizes, Location location) implements Node {
    }
}
