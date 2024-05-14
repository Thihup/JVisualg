package dev.thihup.jvisualg.interpreter;

import dev.thihup.jvisualg.frontend.ASTResult;
import dev.thihup.jvisualg.frontend.Error;
import dev.thihup.jvisualg.frontend.VisualgParser;
import dev.thihup.jvisualg.frontend.node.Location;
import dev.thihup.jvisualg.frontend.node.Node;

import java.io.*;
import java.lang.reflect.Array;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

public class Interpreter {

    public final ArrayDeque<Map<String, Object>> stack = new ArrayDeque<>();
    private final Map<String, Node.FunctionDeclarationNode> functions = new LinkedHashMap<>();
    private final Map<String, Node.ProcedureDeclarationNode> procedures = new LinkedHashMap<>();
    private final RandomGenerator random = RandomGenerator.getDefault();
    private final IO io;

    private boolean aleatorio;
    private boolean eco = true;


    public Interpreter(IO io) {
        this.io = io;
    }

    public void reset() {
        stack.clear();
        functions.clear();
        procedures.clear();
        stack.push(new HashMap<>());
    }

    public CompletableFuture<Void> run(String code, ExecutorService executorService) {

        ASTResult parse = VisualgParser.parse(code);
        if (parse.node().isPresent())
            return CompletableFuture.runAsync(() -> run(parse.node().get()), executorService);
        else
            return CompletableFuture.failedFuture(new Exception("Error parsing code: " + parse.errors().stream().map(x -> x.location() + ":" + x.message()).collect(Collectors.joining("\n"))));
    }

    public void run(Node node) {
        try {
            switch (node) {
                case Node.AlgoritimoNode algoritimoNode -> runAlgoritmo(algoritimoNode);
                case Node.ArrayTypeNode arrayTypeNode -> throw new UnsupportedOperationException("ArrayTypeNode not implemented");
                case Node.CommandNode commandNode -> runCommand(commandNode);
                case Node.CompundNode compundNode -> compundNode.nodes().forEach(this::run);
                case Node.ConstantNode constantNode -> runConstant(constantNode);
                case Node.DosNode dosNode -> {}
                case Node.EmptyNode emptyNode -> {}
                case Node.RegistroDeclarationNode registroDeclarationNode -> throw new UnsupportedOperationException("RegistroDeclarationNode not implemented");
                case Node.SubprogramDeclarationNode subprogramDeclarationNode -> runSubprogramDeclaration(subprogramDeclarationNode);
                case Node.TypeNode typeNode -> throw new UnsupportedOperationException("TypeNode not implemented");
                case Node.VariableDeclarationNode variableDeclarationNode -> runVariableDeclaration(variableDeclarationNode);
                case Node.ExpressionNode e -> evaluate(e);
            }
        } catch (InterruptedException | IOException _) {

        }
    }

    private void runConstant(Node.ConstantNode constantNode) {
        stack.element().put(constantNode.name().id(), evaluate(constantNode.value()));
    }

    private void runSubprogramDeclaration(Node.SubprogramDeclarationNode subprogramDeclarationNode) {
        switch (subprogramDeclarationNode) {
            case Node.FunctionDeclarationNode functionDeclarationNode -> {
                functions.put(functionDeclarationNode.name().id(), functionDeclarationNode);
            }
            case Node.ProcedureDeclarationNode procedureDeclarationNode -> {
                procedures.put(procedureDeclarationNode.name().id(), procedureDeclarationNode);
            }
        }
    }

    private void runCommand(Node.CommandNode commandNode) throws IOException, InterruptedException {
        switch (commandNode) {
            case Node.AleatorioCommandNode aleatorioCommandNode -> aleatorio = aleatorioCommandNode.on();
            case Node.ArquivoCommandNode arquivoCommandNode -> throw new UnsupportedOperationException("ArquivoCommandNode not implemented");
            case Node.AssignmentNode assignmentNode -> runAssignment(assignmentNode);
            case Node.ChooseCaseNode chooseCaseNode -> throw new UnsupportedOperationException("ChooseCaseNode not implemented");
            case Node.ChooseCommandNode chooseCommandNode -> {
                Node.ExpressionNode test = chooseCommandNode.expr();
                for (Node chooseCaseNode : chooseCommandNode.cases().nodes()) {
                    Object evaluate = ((Node.ChooseCaseNode) chooseCaseNode).value();
                    switch (evaluate) {
                        case Node.RangeNode(Node.ExpressionNode start, Node.ExpressionNode end, _) -> {
                            if (((Number) evaluate(test)).intValue() >= ((Number) evaluate(start)).intValue() && ((Number) evaluate(test)).intValue() <= ((Number) evaluate(end)).intValue()) {
                                run(((Node.ChooseCaseNode) chooseCaseNode).commands());
                                return;
                            }
                        }
                        case Node.ExpressionNode e -> {
                            if ((Boolean) evaluate(new Node.EqNode(test, e, Optional.empty()))) {
                                run(((Node.ChooseCaseNode) chooseCaseNode).commands());
                                return;
                            }
                        }
                        default -> throw new UnsupportedOperationException("Unsupported type: " + evaluate.getClass());
                    }
                }
                switch (chooseCommandNode.defaultCase()) {
                    case Node.EmptyNode _ -> {}
                    case Node.ChooseCaseNode(_, var commands, _) -> run(commands);
                    default -> throw new UnsupportedOperationException("Unsupported type: " + chooseCommandNode.defaultCase().getClass());
                }
            }
            case Node.ConditionalCommandNode conditionalCommandNode -> runConditionalCommand(conditionalCommandNode);
            case Node.CronometroCommandNode cronometroCommandNode -> {}
            case Node.DebugCommandNode debugCommandNode -> throw new UnsupportedOperationException("DebugCommandNode not implemented");
            case Node.EcoCommandNode ecoCommandNode -> eco = ecoCommandNode.on();
            case Node.ForCommandNode forCommandNode -> runForCommand(forCommandNode);
            case Node.IncrementNode incrementNode -> throw new UnsupportedOperationException("IncrementNode not implemented");
            case Node.InterrompaCommandNode interrompaCommandNode -> runInterrompaCommand(interrompaCommandNode);
            case Node.LimpatelaCommandNode limpatelaCommandNode -> {}
            case Node.PausaCommandNode pausaCommandNode -> {}
            case Node.ProcedureCallNode procedureCallNode -> {
                Node.ProcedureDeclarationNode procedureDeclaration = procedures.get(procedureCallNode.name().id());
                if (procedureDeclaration != null) {
                    stack.push(new HashMap<>());
                    List<Node> parameters = procedureDeclaration.parameters().nodes();
                    List<Node> arguments = procedureCallNode.args().nodes();
                    if (parameters.size() != arguments.size()) {
                        throw new UnsupportedOperationException("Expected " + parameters.size() + " arguments but got " + arguments.size());
                    }
                    for (int i = 0; i < arguments.size(); i++){
                        assignVariable(((Node.VariableDeclarationNode) parameters.get(i)).name().id(), evaluate((Node.ExpressionNode) arguments.get(i)));
                    }
                    run(procedureDeclaration.declarations());
                    run(procedureDeclaration.commands());
                    stack.pop();
                } else if (procedureCallNode.name().id().equals("mudacor")) {
                } else {
                    throw new UnsupportedOperationException("Procedure not found: " + procedureCallNode.name().id());
                }

            }
            case Node.ReadCommandNode readCommandNode -> runReadCommand(readCommandNode);
            case Node.ReturnNode returnNode -> {
                stack.element().put("(RESULTADO)", evaluate(returnNode.expr()));
                throw new ReturnException();
            }
            case Node.TimerCommandNode timerCommandNode -> {}
            case Node.WhileCommandNode whileCommandNode -> runWhileCommand(whileCommandNode);
            case Node.WriteCommandNode writeCommandNode -> runWriteCommandNode(writeCommandNode);
            case Node.WriteItemNode(Node.ExpressionNode expr, Node spaces, Node precision, _) -> runWriteItemNode(expr, spaces, precision);
        }
    }

    private static class BreakException extends RuntimeException {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    private static class ReturnException extends RuntimeException {
        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    private void runInterrompaCommand(Node.InterrompaCommandNode interrompaCommandNode) {
        throw new BreakException();
    }

    private void runAssignment(Node.AssignmentNode assignmentNode) {
        Object evaluate = evaluate(assignmentNode.expr());
        switch (assignmentNode.idOrArray()) {
            case Node.IdNode idNode -> assignVariable(idNode.id(), evaluate);
            case Node.ArrayAccessNode arrayAccessNode -> {
                Node node = arrayAccessNode.node();
                Object o = getVariableFromStack((Node.IdNode) node);
                Node.CompundNode indexes = arrayAccessNode.indexes();
                switch(indexes.nodes().size()) {
                    case 1 -> {
                        int index = ((Number) evaluate((Node.ExpressionNode) indexes.nodes().getFirst())).intValue();
                        if (o instanceof Double[] && evaluate instanceof Integer i)
                            evaluate = i.doubleValue();
                        Array.set(o, index, evaluate);
                    }
                    case 2 -> {
                        int index1 = ((Number) evaluate((Node.ExpressionNode) indexes.nodes().getFirst())).intValue();
                        int index2 = ((Number) evaluate((Node.ExpressionNode) indexes.nodes().getLast())).intValue();
                        Object array = Array.get(o, index1);
                        if (array instanceof Double[] && evaluate instanceof Integer i)
                            evaluate = i.doubleValue();

                        Array.set(array, index2, evaluate);
                    }
                    default -> throw new UnsupportedOperationException("Unsupported number of indexes: " + indexes.nodes().size());
                }

            }
            case null, default -> throw new UnsupportedOperationException("Unsupported type: " + assignmentNode);
        }
    }

    private void runWriteItemNode(Node.ExpressionNode expr, Node spaces, Node precision) throws IOException {
        printValue(evaluate(expr), spaces, precision);
    }

    private void runWriteCommandNode(Node.WriteCommandNode writeCommandNode) throws IOException {
        run(writeCommandNode.writeList());
        if (writeCommandNode.newLine()) {
            io.output().accept("\n");
        }
    }

    private void runWhileCommand(Node.WhileCommandNode whileCommandNode) {
        try {
            if (whileCommandNode.conditionAtEnd()) {
                do {
                    run(whileCommandNode.commands());
                } while (!(Boolean) evaluate(whileCommandNode.test()));
            } else {
                while ((Boolean) evaluate(whileCommandNode.test())) {
                    run(whileCommandNode.commands());
                }
            }
        } catch (BreakException _) {
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

    private void runReadCommand(Node.ReadCommandNode readCommandNode) throws IOException {

        readCommandNode.exprList().nodes().forEach(expr -> {
            switch (expr) {
                case Node.IdNode idNode -> {
                    Object o = getVariableFromStack(idNode);
                    final InputValue inputValue = aleatorio ? null : io.input().apply(new InputRequestValue(idNode.id(), InputRequestValue.Type.fromClass(o.getClass()))).join();
                    Object value = switch (o) {
                        case Boolean _ when aleatorio -> random.nextBoolean();
                        case String _ when aleatorio -> random.ints(65, 91)
                                .limit(5)
                                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
                        case Double _ when aleatorio -> random.nextDouble(100);
                        case Integer _ when aleatorio -> random.nextInt(100);

                        case Integer _ when inputValue instanceof InputValue.InteiroValue(var value1) -> value1;
                        case Double _ when inputValue instanceof InputValue.RealValue(var value1) -> value1;
                        case String _ when inputValue instanceof InputValue.CaracterValue(var value1) -> value1;
                        case Boolean _ when inputValue instanceof InputValue.LogicoValue(var value1) -> value1;

                        default -> throw new UnsupportedOperationException("Unsupported type: " + o.getClass());
                    };
                    if (eco)
                        io.output().accept(value + "\n");

                    assignVariable(idNode.id(), value);
                }
                case Node.ArrayAccessNode arrayAccessNode -> {
                    Node.IdNode node = (Node.IdNode) arrayAccessNode.node();
                    Object o = getVariableFromStack(node);

                    Node.CompundNode indexes = arrayAccessNode.indexes();
                    switch(indexes.nodes().size()) {
                        case 1 -> {
                            int index = ((Number) evaluate((Node.ExpressionNode) indexes.nodes().getFirst())).intValue();
                            final InputValue inputValue = aleatorio ? null : io.input().apply(new InputRequestValue(node.id() + "[" + index + "]", InputRequestValue.Type.fromClass(o.getClass().getComponentType()))).join();
                            Array.set(o, index, readValueForArray(o, inputValue));
                        }
                        case 2 -> {
                            int index1 = ((Number) evaluate((Node.ExpressionNode) indexes.nodes().getFirst())).intValue();
                            int index2 = ((Number) evaluate((Node.ExpressionNode) indexes.nodes().getLast())).intValue();
                            final InputValue inputValue = aleatorio ? null : io.input().apply(new InputRequestValue(node.id() + "[" + index1 + "," + index2 + "]", InputRequestValue.Type.fromClass(o.getClass().getComponentType().getComponentType()))).join();
                            Array.set(Array.get(o, index1), index2, readValueForArray(Array.get(o, index1), inputValue));
                        }
                        default -> throw new UnsupportedOperationException("Unsupported number of indexes: " + indexes.nodes().size());
                    }
                }
                case null, default -> throw new UnsupportedOperationException("Unsupported type: " + expr.getClass());
            }
        });

    }

    private Object readValueForArray(Object o, InputValue inputValue) {
        Object o1 = switch (o) {
            case Boolean[] _ when aleatorio -> random.nextBoolean();
            case String[] _ when aleatorio -> random.ints(65, 91)
                    .limit(5)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append);
            case Double[] _ when aleatorio -> random.nextDouble(100);
            case Integer[] _ when aleatorio -> random.nextInt(100);

            case Integer[] _ when inputValue instanceof InputValue.InteiroValue(var value1) -> value1;
            case Double[] _ when inputValue instanceof InputValue.RealValue(var value1) -> value1;
            case String[] _ when inputValue instanceof InputValue.CaracterValue(var value1) -> value1;
            case Boolean[] _ when inputValue instanceof InputValue.LogicoValue(var value1) -> value1;
            default -> throw new UnsupportedOperationException("Unsupported type: " + o.getClass());
        };
        if (eco)
            io.output().accept(o1.toString() + "\n");
        return o1;
    }

    private Object getVariableFromStack(Node.IdNode idNode) {
        return stack.stream().map(m -> m.get(idNode.id())).filter(Objects::nonNull).findFirst().orElseThrow(() -> new UnsupportedOperationException("Variable not found: " + idNode.id()));
    }

    private void runForCommand(Node.ForCommandNode forCommandNode) {
        Node.IdNode identifier = forCommandNode.identifier();

        switch(forCommandNode) {
            case Node.ForCommandNode(Node.IdNode id, Node.ExpressionNode start, Node.ExpressionNode end, Node.ExpressionNode step, Node.CompundNode command, _) -> {
                Object variableFromStack = getVariableFromStack(identifier);
                int startValue = ((Number)evaluate(start)).intValue();
                int endValue = ((Number)evaluate(end)).intValue();;
                int stepValue = ((Number)evaluate(step)).intValue();
                int i;
                if (stepValue < 0) {
                    for (i = startValue; i >= endValue; i += stepValue) {
                        assignVariable(id.id(), i);
                        try {
                            run(command);
                        } catch (BreakException _) {
                        }
                    }
                } else {
                    for (i = startValue; i <= endValue; i += stepValue) {
                        assignVariable(id.id(), i);
                        try {
                            run(command);
                        } catch (BreakException _) {
                        }
                    }
                }
                assignVariable(id.id(), i);
            }
            case Node.ForCommandNode(Node.IdNode id, Node.ExpressionNode start, Node.EmptyNode end, Node.ExpressionNode step, Node.CompundNode command, _) -> {
            }
            default -> throw new UnsupportedOperationException("Unsupported type: " + forCommandNode.getClass());
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
        io.output().accept(switch (value) {
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
            case null -> "NULL??!!";
            default -> throw new UnsupportedOperationException("Unsupported type: " + value.getClass());
        });

    }

    private Object evaluate(Node.ExpressionNode node){
        return switch (node) {
            case Node.StringLiteralNode(var value, _) -> value;
            case Node.BinaryNode binaryNode -> evaluateBinaryNode(binaryNode);
            case Node.BooleanLiteralNode(var value, _) -> value;
            case Node.FunctionCallNode functionCallNode -> evaluateFunction(functionCallNode);
            case Node.IntLiteralNode(var value, _) -> value;
            case Node.RealLiteralNode(var value, _) -> value;
            case Node.IdNode idNode -> getVariableFromStack(idNode);
            case Node.NegNode nedNode -> switch (evaluate(nedNode.expr())) {
                case Double x -> -x;
                case Integer x -> -x;
                case Object o -> throw new UnsupportedOperationException("unsupported neg node types: " + o);
            };
            case Node.PosNode(Node.ExpressionNode e, _) -> evaluate(e);
            case Node.NotNode notNode -> !(Boolean) evaluate(notNode.expr());
            case Node.EmptyExpressionNode _ -> 0;
            case Node.ArrayAccessNode arrayAccessNode -> evaluateArrayAccessNode(arrayAccessNode);
            case Node.MemberAccessNode memberAccessNode -> throw new UnsupportedOperationException("MemberAccessNode not implemented");
            case Node.RangeNode rangeNode -> throw new UnsupportedOperationException("MemberAccessNode not implemented");
        };
    }

    private Object evaluateArrayAccessNode(Node.ArrayAccessNode arrayAccessNode) {
        Node node = arrayAccessNode.node();
        Object o = getVariableFromStack((Node.IdNode) node);

        Node.CompundNode indexes = arrayAccessNode.indexes();
        switch(indexes.nodes().size()) {
            case 1 -> {
                int index = ((Number) evaluate((Node.ExpressionNode) indexes.nodes().getFirst())).intValue();
                return Array.get(o, index);
            }
            case 2 -> {
                int index1 = ((Number) evaluate((Node.ExpressionNode) indexes.nodes().getFirst())).intValue();
                int index2 = ((Number) evaluate((Node.ExpressionNode) indexes.nodes().getLast())).intValue();
                return Array.get(Array.get(o, index1), index2);
            }
            default -> throw new UnsupportedOperationException("Unsupported number of indexes: " + indexes.nodes().size());
        }
    }

    private Object evaluateFunction(Node.FunctionCallNode functionCallNode) {
        Node.FunctionDeclarationNode functionDeclaration = functions.get(functionCallNode.name().id());
        if (functionDeclaration != null) {
            stack.push(new HashMap<>());
            stack.element().put("(RESULTADO)", newInstance(functionDeclaration.returnType()));
            List<Node> parameters = functionDeclaration.parameters().nodes();
            List<Node> arguments = functionCallNode.args().nodes();
            if (parameters.size() != arguments.size()) {
                throw new UnsupportedOperationException("Expected " + parameters.size() + " arguments but got " + arguments.size());
            }
            for (int i = 0; i < arguments.size(); i++){
                stack.element().put(((Node.VariableDeclarationNode) parameters.get(i)).name().id(), evaluate((Node.ExpressionNode) arguments.get(i)));
            }
            run(functionDeclaration.declarations());
            try {
                run(functionDeclaration.commands());
            } catch (ReturnException _) {
            }
            Object result = stack.element().get("(RESULTADO)");
            stack.pop();
            return result;
        } else if (StandardFunctions.FUNCTIONS.containsKey(functionCallNode.name().id())) {
            try {
                return StandardFunctions.FUNCTIONS.get(functionCallNode.name().id()).invokeWithArguments(functionCallNode.args().nodes().stream().map(node -> evaluate((Node.ExpressionNode) node)).toList());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new UnsupportedOperationException("Function not found: " + functionCallNode.name().id());
        }
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
                    case PairValue(String x, String y) -> x + y;
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
                    case PairValue(String x, String y) -> x.compareToIgnoreCase(y) >= 0;
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
                    case PairValue(String x, String y) -> x.compareToIgnoreCase(y) > 0;
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
                    case PairValue(String x, String y) -> x.compareToIgnoreCase(y) <= 0;
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
                    case PairValue(String x, String y) -> x.compareToIgnoreCase(y) < 0;
                    case Object o -> throw new UnsupportedOperationException("unsupported lt node types: " + o);
                };
            }

            case Node.EqNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Boolean _, Number y) -> y;
                    case PairValue(Number _, Boolean y) -> y;
                    case PairValue(String x, String y) -> x.equalsIgnoreCase(y);
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
                    case PairValue(String x, String y) -> !x.equalsIgnoreCase(y);
                    case PairValue(Object x, Object y) -> !x.equals(y);
                    case Object o -> throw new UnsupportedOperationException("unsupported ne node types: " + o);
                };
            }
        };
    }

    public void stop() {
        Thread.currentThread().interrupt();
    }

    private void runAlgoritmo(Node.AlgoritimoNode algoritimoNode) throws InterruptedException {
        stack.push(new HashMap<>());
        run(algoritimoNode.declarations());
        run(algoritimoNode.commands());
    }

    private void runVariableDeclaration(Node.VariableDeclarationNode variableDeclarationNode) {
        stack.element().put(variableDeclarationNode.name().id(), newInstance(variableDeclarationNode.type()));
    }

    private void assignVariable(String name, Object value) {
        stack.stream().filter(m -> m.containsKey(name)).findFirst().ifPresentOrElse(m -> m.put(name, value), () -> {
            throw new UnsupportedOperationException("Variable not found: " + name);
        });
    }

    private Class<?> getType(Node typeNode) {
        return switch (typeNode) {
            case Node.TypeNode(Node.StringLiteralNode(var type, _), _) -> switch (type.toLowerCase()) {
                case "inteiro" -> Integer.class;
                case "real", "numerico" -> Double.class;
                case "caracter", "caractere", "literal" -> String.class;
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
                case "real", "numerico" -> 0.0;
                case "caracter", "caractere", "literal" -> "";
                case "logico" -> false;
                default -> throw new IllegalStateException("Unexpected value: " + type);
            };
            case Node.ArrayTypeNode(Node.TypeNode type, Node.CompundNode sizes, Optional<Location> location) -> {
                Class<?> typeClass = getType(type);
                int[] dimensions = sizes.nodes().stream()
                        .map(Node.RangeNode.class::cast)
                        .mapToInt(node -> (Integer)evaluate(node.end())+ 1)
                        .toArray();
                Object o = Array.newInstance(typeClass, dimensions);
                switch (o) {
                    case String[] stringArray -> Arrays.fill(stringArray, newInstance(type));
                    case Integer[] intArray -> Arrays.fill(intArray, newInstance(type));
                    case Double[] doubleArray -> Arrays.fill(doubleArray, newInstance(type));
                    case Boolean[] booleanArray -> Arrays.fill(booleanArray, newInstance(type));

                    case String[][] stringArray ->
                            Arrays.stream(stringArray).forEach(x -> Arrays.fill(x, newInstance(type)));
                    case Integer[][] intArray -> Arrays.stream(intArray).forEach(x -> Arrays.fill(x, newInstance(type)));
                    case Double[][] doubleArray -> Arrays.stream(doubleArray).forEach(x -> Arrays.fill(x, newInstance(type)));
                    case Boolean[][] booleanArray -> Arrays.stream(booleanArray).forEach(x -> Arrays.fill(x, newInstance(type)));
                    default -> throw new IllegalStateException("Unexpected value: " + o);
                }
                yield o;
            }
            default -> throw new IllegalStateException("Unexpected value: " + typeNode);
        };
    }

}
