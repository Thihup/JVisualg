package dev.thihup.jvisualg.interpreter;

import dev.thihup.jvisualg.frontend.node.Location;
import dev.thihup.jvisualg.frontend.node.Node;

import java.io.*;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.locks.Lock;

public class Interpreter {

    private final Map<String, Object> global = new LinkedHashMap<>();
    private final Reader originalInput;
    private final Writer output;
    private final Lock lock;
    private boolean running = true;
    private Scanner input;

    private List<Location> breakpointLocations = new ArrayList<>();

    Interpreter(Reader input, Writer output, Lock debuggerLock) {
        this.originalInput = input;
        this.input = new Scanner(input);
        this.output = output;
        this.lock = debuggerLock;
    }

    public void run(Node node) {
        if (!running) {
            return;
        }
        try {
            if (breakpointLocations.stream()
                    .anyMatch(location -> location.isInside(node.location().orElse(Location.EMPTY)))) {
                lock.lock();
            }

            switch (node) {
                case Node.AlgoritimoNode algoritimoNode -> runAlgoritmo(algoritimoNode);
                case Node.ArrayTypeNode arrayTypeNode -> throw new UnsupportedOperationException("ArrayTypeNode not implemented");
                case Node.CommandNode commandNode -> runCommand(commandNode);
                case Node.CompundNode compundNode -> compundNode.nodes().forEach(this::run);
                case Node.ConstantNode constantNode -> throw new UnsupportedOperationException("ConstantNode not implemented");
                case Node.DosNode dosNode -> {}
                case Node.EmptyNode emptyNode -> {}
                case Node.IdNode idNode -> throw new UnsupportedOperationException("IdNode not implemented");
                case Node.LiteralNode literalNode -> throw new UnsupportedOperationException("LiteralNode not implemented");
                case Node.RangeNode rangeNode -> throw new UnsupportedOperationException("RangeNode not implemented");
                case Node.RegistroDeclarationNode registroDeclarationNode -> throw new UnsupportedOperationException("RegistroDeclarationNode not implemented");
                case Node.SubprogramDeclarationNode subprogramDeclarationNode -> throw new UnsupportedOperationException("SubprogramDeclarationNode not implemented");
                case Node.TypeNode typeNode -> throw new UnsupportedOperationException("TypeNode not implemented");
                case Node.VariableDeclarationNode variableDeclarationNode -> runVariableDeclaration(variableDeclarationNode);
                case Node.ExpressionNode e -> evaluate(e);
            }
        } catch (InterruptedException | IOException _) {
            running = false;
        }
    }

    private void runCommand(Node.CommandNode commandNode) throws IOException, InterruptedException {
        switch (commandNode) {
            case Node.AleatorioCommandNode aleatorioCommandNode -> throw new UnsupportedOperationException("AleatorioCommandNode not implemented");
            case Node.ArquivoCommandNode arquivoCommandNode -> throw new UnsupportedOperationException("ArquivoCommandNode not implemented");
            case Node.AssignmentNode assignmentNode -> throw new UnsupportedOperationException("AssignmentNode not implemented");
            case Node.BinaryNode binaryNode -> throw new UnsupportedOperationException("BinaryNode not implemented");
            case Node.BooleanNode booleanNode -> throw new UnsupportedOperationException("BooleanNode not implemented");
            case Node.ChooseCaseNode chooseCaseNode -> throw new UnsupportedOperationException("ChooseCaseNode not implemented");
            case Node.ChooseCommandNode chooseCommandNode -> throw new UnsupportedOperationException("ChooseCommandNode not implemented");
            case Node.ConditionalCommandNode conditionalCommandNode -> runConditionalCommand(conditionalCommandNode);
            case Node.CronometroCommandNode cronometroCommandNode -> throw new UnsupportedOperationException("CronometroCommandNode not implemented");
            case Node.DebugCommandNode debugCommandNode -> throw new UnsupportedOperationException("DebugCommandNode not implemented");
            case Node.EcoCommandNode ecoCommandNode -> throw new UnsupportedOperationException("EcoCommandNode not implemented");
            case Node.ForCommandNode forCommandNode -> runForCommand(forCommandNode);
            case Node.FunctionCallNode functionCallNode -> throw new UnsupportedOperationException("FunctionCallNode not implemented");
            case Node.IncrementNode incrementNode -> throw new UnsupportedOperationException("IncrementNode not implemented");
            case Node.InterrompaCommandNode interrompaCommandNode -> throw new UnsupportedOperationException("InterrompaCommandNode not implemented");
            case Node.LimpatelaCommandNode limpatelaCommandNode -> throw new UnsupportedOperationException("LimpatelaCommandNode not implemented");
            case Node.PausaCommandNode pausaCommandNode -> throw new UnsupportedOperationException("PausaCommandNode not implemented");
            case Node.ProcedureCallNode procedureCallNode -> throw new UnsupportedOperationException("ProcedureCallNode not implemented");
            case Node.ReadCommandNode readCommandNode -> runReadCommand(readCommandNode);
            case Node.ReturnNode returnNode -> throw new UnsupportedOperationException("ReturnNode not implemented");
            case Node.TimerCommandNode timerCommandNode -> throw new UnsupportedOperationException("TimerCommandNode not implemented");
            case Node.WhileCommandNode whileCommandNode -> throw new UnsupportedOperationException("WhileCommandNode not implemented");
            case Node.WriteCommandNode writeCommandNode -> {
                run(writeCommandNode.writeList());
                if (writeCommandNode.newLine()) {
                    output.write("\n");
                }
                output.flush();
            }
            case Node.WriteItemNode(Node.ExpressionNode expr, Node spaces, Node precision, _) -> {
                Object value = evaluate(expr);
                printValue(value, spaces, precision);
            }
            case Node.WriteItemNode(Node a, _, _, _) -> throw new UnsupportedOperationException("the value to write must be an expression: " + a.getClass());
        }
    }

    private void runConditionalCommand(Node.ConditionalCommandNode conditionalCommandNode) {
        if (conditionalCommandNode instanceof Node.ConditionalCommandNode(Node.ExpressionNode expressionNode, Node.CompundNode command, Node.CompundNode elseCommand, _)) {
            if ((Boolean) evaluate(expressionNode)) {
                run(command);
            } else {
                run(elseCommand);
            }
        } else {
            throw new UnsupportedOperationException("Test should be an expression: " + conditionalCommandNode);
        }
    }

    private void runReadCommand(Node.ReadCommandNode readCommandNode) {

        readCommandNode.exprList().nodes().forEach(expr -> {

            if (expr instanceof Node.IdNode idNode) {
                Object o = global.get(idNode.id());
                switch (o) {
                    case Integer _ when input.hasNextInt() -> global.put(idNode.id(), input.nextInt());
                    case Double _ when input.hasNextDouble() -> global.put(idNode.id(), input.nextDouble());
                    case String _ when input.hasNextLine() -> global.put(idNode.id(), input.nextLine());
                    case Boolean _ when input.hasNextBoolean() -> global.put(idNode.id(), input.nextBoolean());
                    default -> throw new UnsupportedOperationException("Unsupported type: " + o.getClass());
                }
            } else {
                throw new UnsupportedOperationException("Unsupported type: " + expr.getClass());
            }
        });

    }

    private void runForCommand(Node.ForCommandNode forCommandNode) {
        Node.IdNode identifier = forCommandNode.identifier();
        if (global.containsKey(identifier.id())) {
            switch(forCommandNode) {
                case Node.ForCommandNode(Node.IdNode id, Node.ExpressionNode start, Node.ExpressionNode end, Node.ExpressionNode step, Node.CompundNode command, _) -> {
                    int startValue = ((Number)evaluate(start)).intValue();
                    int endValue = ((Number)evaluate(end)).intValue();;
                    int stepValue = ((Number)evaluate(step)).intValue();
                    int i;
                    for (i = startValue; i <= endValue; i += stepValue) {
                        global.put(id.id(), i);
                        run(command);
                    }
                    global.put(id.id(), i);
                }
                default -> throw new UnsupportedOperationException("Unsupported type: " + forCommandNode.getClass());
            }
        }
    }

    private void printValue(Object value, Node spaces, Node precision) throws IOException {

        if (value instanceof Integer i) {
            value = i.doubleValue();
        }

        int spacesValue = switch (spaces) {
            case Node.ExpressionNode e when evaluate(e) instanceof Number p -> p.intValue();
            case Node.EmptyNode _ -> 0;
            default -> throw new UnsupportedOperationException("Unsupported type: " + precision.getClass());
        };

        int precisionValue = switch (precision) {
            case Node.ExpressionNode e when evaluate(e) instanceof Number p -> p.intValue();
            case Node.EmptyNode _ -> 0;
            default -> throw new UnsupportedOperationException("Unsupported type: " + precision.getClass());
        };

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setRoundingMode(RoundingMode.HALF_UP);
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setMinimumFractionDigits(0);
        output.write(switch (value) {
            case Double doubleValue when spacesValue >= 1 && precisionValue >= 1 -> {
                numberFormat.setMinimumFractionDigits(precisionValue);
                numberFormat.setMaximumFractionDigits(precisionValue);
                yield numberFormat.format(doubleValue);
            }
            case Double doubleValue when spacesValue >= 1 && precisionValue == 0 -> {
                numberFormat.setMaximumFractionDigits(0);
                numberFormat.setMinimumFractionDigits(0);
                yield " ".repeat(spacesValue - 1) + numberFormat.format(doubleValue);
            }
            case Double doubleValue -> {
                numberFormat.setMinimumFractionDigits(0);
                numberFormat.setMaximumFractionDigits(99999);
                yield " " + numberFormat.format(doubleValue);
            }

            case String string -> spacesValue > 0 ? string.substring(0, spacesValue) : string;
            case Boolean bool -> bool ? " VERDADEIRO" : " FALSO";
            default -> throw new UnsupportedOperationException("Unsupported type: " + value.getClass());
        });
    }

    private Object evaluate(Node.ExpressionNode node){
        return switch (node) {
            case Node.StringLiteralNode(var value, _) -> value;
            case Node.BinaryNode binaryNode -> evaluateBinaryNode(binaryNode);
            case Node.BooleanLiteralNode(var value, _) -> value;
            case Node.FunctionCallNode functionCallNode -> throw new UnsupportedOperationException("FunctionCallNode not implemented");
            case Node.IntLiteralNode(var value, _) -> value;
            case Node.RealLiteralNode(var value, _) -> value;
            case Node.IdNode(String id, _) -> global.get(id);
            case Node.NegNode nedNode -> throw new UnsupportedOperationException("NegNode not implemented");
            case Node.PosNode(Node.ExpressionNode e, _) -> evaluate(e);
            case Node.NotNode notNode -> throw new UnsupportedOperationException("NotNode not implemented");
            case Node.EmptyExpressionNode _ -> 0;
            case Node.ArrayAccessNode arrayAccessNode -> throw new UnsupportedOperationException("ArrayAccessNode not implemented");
            case Node.MemberAccessNode memberAccessNode -> throw new UnsupportedOperationException("MemberAccessNode not implemented");
        };
    }
    record PairValue(Object left, Object right) {}
    private Object evaluateBinaryNode(Node.BinaryNode binaryNode) {

        return switch (binaryNode) {
            case Node.AddNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Number x, Double y) -> x.doubleValue() + y;
                    case PairValue(Double x, Number y) -> x + y.doubleValue();

                    case PairValue(Number x, Number y) -> x.intValue() + y.intValue();
                    case Object o -> throw new UnsupportedOperationException("unsupported add node types: " + o);
                };
            }
            case Node.DivNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Number x, Double y) -> x.doubleValue() / y;
                    case PairValue(Double x, Number y) -> x / y.doubleValue();

                    case PairValue(Number x, Number y) -> x.intValue() / y.intValue();
                    case Object o -> throw new UnsupportedOperationException("unsupported div node types: " + o);
                };
            }
            case Node.ModNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Number x, Double y) -> x.doubleValue() % y;
                    case PairValue(Double x, Number y) -> x % y.doubleValue();

                    case PairValue(Number x, Number y) -> x.intValue() % y.intValue();
                    case Object o -> throw new UnsupportedOperationException("unsupported mod node types: " + o);
                };
            }
            case Node.MulNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Number x, Double y) -> x.doubleValue() * y;
                    case PairValue(Double x, Number y) -> x * y.doubleValue();

                    case PairValue(Number x, Number y) -> x.intValue() * y.intValue();
                    case Object o -> throw new UnsupportedOperationException("unsupported mul node types: " + o);
                };
            }

            case Node.PowNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Number x, Double y) -> Math.pow(x.doubleValue(),y);
                    case PairValue(Double x, Number y) -> Math.pow(x, y.doubleValue());

                    case PairValue(Number x, Number y) -> (int) Math.pow(x.intValue(), y.intValue());
                    case Object o -> throw new UnsupportedOperationException("unsupported pow node types: " + o);
                };
            }
            case Node.SubNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Number x, Double y) -> x.doubleValue() - y;
                    case PairValue(Double x, Number y) -> x - y.doubleValue();

                    case PairValue(Number x, Number y) -> x.intValue() - y.intValue();
                    case Object o -> throw new UnsupportedOperationException("unsupported sub node types: " + o);
                };
            }
            case Node.RelationalNode relationalNode -> evaluateRelationalNode(relationalNode);
        };
    }

    private Object evaluateRelationalNode(Node.RelationalNode relationalNode) {
        return switch (relationalNode) {
            case Node.AndNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Boolean x, Boolean y) -> x && y;
                    case Object o -> throw new UnsupportedOperationException("unsupported and node types: " + o);
                };
            }
            case Node.OrNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Boolean x, Boolean y) -> x || y;
                    case Object o -> throw new UnsupportedOperationException("unsupported or node types: " + o);
                };
            }

            case Node.GeNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Number x, Double y) -> x.doubleValue() >= y;
                    case PairValue(Double x, Number y) -> x >= y.doubleValue();
                    case PairValue(Boolean x, Boolean y) -> x || !y;
                    case PairValue(Boolean _, Number y) -> y;
                    case PairValue(Number _, Boolean y) -> y;

                    case PairValue(Number x, Number y) -> x.intValue() >= y.intValue();
                    case Object o -> throw new UnsupportedOperationException("unsupported ge node types: " + o);
                };
            }
            case Node.GtNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Number x, Double y) -> x.doubleValue() > y;
                    case PairValue(Double x, Number y) -> x > y.doubleValue();
                    case PairValue(Boolean x, Boolean y) -> x && !y;
                    case PairValue(Number x, Number y) -> x.intValue() > y.intValue();
                    case PairValue(Boolean _, Number y) -> y;
                    case PairValue(Number _, Boolean y) -> y;


                    case Object o -> throw new UnsupportedOperationException("unsupported gt node types: " + o);
                };
            }
            case Node.LeNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Number x, Double y) -> x.doubleValue() <= y;
                    case PairValue(Double x, Number y) -> x <= y.doubleValue();
                    case PairValue(Boolean x, Boolean y) -> !x || y;
                    case PairValue(Boolean _, Number y) -> y;
                    case PairValue(Number _, Boolean y) -> y;


                    case PairValue(Number x, Number y) -> x.intValue() <= y.intValue();
                    case Object o -> throw new UnsupportedOperationException("unsupported le node types: " + o);
                };
            }
            case Node.LtNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Number x, Double y) -> x.doubleValue() < y;
                    case PairValue(Double x, Number y) -> x < y.doubleValue();
                    case PairValue(Boolean x, Boolean y) -> !x && y;
                    case PairValue(Boolean _, Number y) -> y;
                    case PairValue(Number _, Boolean y) -> y;


                    case PairValue(Number x, Number y) -> x.intValue() < y.intValue();
                    case Object o -> throw new UnsupportedOperationException("unsupported lt node types: " + o);
                };
            }

            case Node.EqNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Boolean _, Number y) -> y;
                    case PairValue(Number _, Boolean y) -> y;
                    case PairValue(Object x, Object y) -> x.equals(y);
                    case Object o -> throw new UnsupportedOperationException("unsupported eq node types: " + o);
                };
            }

            case Node.NeNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Boolean _, Number y) -> y;
                    case PairValue(Number _, Boolean y) -> y;
                    case PairValue(Object x, Object y) -> !x.equals(y);
                    case Object o -> throw new UnsupportedOperationException("unsupported ne node types: " + o);
                };
            }
        };
    }

    public void stop() {
        running = false;
        Thread.currentThread().interrupt();
    }

    private void runAlgoritmo(Node.AlgoritimoNode algoritimoNode) throws InterruptedException {
        run(algoritimoNode.declarations());
        run(algoritimoNode.commands());
    }

    private void runVariableDeclaration(Node.VariableDeclarationNode variableDeclarationNode) {
        global.put(variableDeclarationNode.name().id(), newInstance(variableDeclarationNode.type()));
    }

    private Class<?> getType(Node typeNode) {
        return switch (typeNode) {
            case Node.TypeNode(Node.StringLiteralNode(var type, _), _) -> switch (type.toLowerCase()) {
                case "inteiro" -> Integer.class;
                case "real" -> Double.class;
                case "caracter" -> String.class;
                case "logico" -> Boolean.class;
                default -> throw new IllegalStateException("Unexpected value: " + type);
            };
            default -> throw new IllegalStateException("Unexpected value: " + typeNode);
        };
    }

    private Object newInstance(Node typeNode) {
        return switch (typeNode) {
            case Node.TypeNode(Node.StringLiteralNode(var type, _), _) -> switch (type.toLowerCase()) {
                case "inteiro" -> 0;
                case "real" -> 0.0;
                case "caracter" -> "";
                case "logico" -> false;
                default -> throw new IllegalStateException("Unexpected value: " + type);
            };
            default -> throw new IllegalStateException("Unexpected value: " + typeNode);
        };
    }

    public void addBreakpoint(Location location) {
        breakpointLocations.add(location);
    }
}
