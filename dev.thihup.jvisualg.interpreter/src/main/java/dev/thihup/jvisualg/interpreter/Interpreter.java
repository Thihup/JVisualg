package dev.thihup.jvisualg.interpreter;

import dev.thihup.jvisualg.frontend.ASTResult;
import dev.thihup.jvisualg.frontend.VisualgParser;
import dev.thihup.jvisualg.frontend.node.Location;
import dev.thihup.jvisualg.frontend.node.Node;

import java.io.IOException;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

public class Interpreter {

    public final SequencedMap<String, Map<String, Object>> stack = new LinkedHashMap<>();
    private final Map<String, Node.FunctionDeclarationNode> functions = new LinkedHashMap<>();
    private final Map<String, Node.ProcedureDeclarationNode> procedures = new LinkedHashMap<>();
    private final RandomGenerator random = RandomGenerator.getDefault();
    private final IO io;
    private final Consumer<ProgramState> debuggerCallback;

    private AleatorioState aleatorio = AleatorioState.Off.INSTANCE;
    private boolean eco = true;
    private List<Integer> breakpoints = new ArrayList<>();

    public enum State {
        RUNNING,
        PAUSED,
        STOPPED
    }
    private State state = State.STOPPED;

    private sealed interface AleatorioState {
        record Range(int start, int end, int decimalPlaces) implements AleatorioState {}
        record Off() implements AleatorioState {
            static final Off INSTANCE = new Off();
        }
    }


    public record ProgramState(int lineNumber, Map<String, Map<String, Object>> stack) {
    }

    public Interpreter(IO io, Consumer<ProgramState> debuggerCallback) {
        this.io = io;
        this.debuggerCallback = debuggerCallback;
    }

    public Interpreter(IO io) {
        this(io, null);
    }

    public void addBreakpoint(int location) {
        this.breakpoints.add(location);
    }

    public void reset() {
        stack.clear();
        functions.clear();
        procedures.clear();
        stack.clear();
        breakpoints.clear();
        state = State.STOPPED;
    }

    public State state() {
        return state;
    }

    public CompletableFuture<Void> run(String code, ExecutorService executorService) {
        state = State.RUNNING;
        ASTResult parse = VisualgParser.parse(code);
        CompletableFuture<Void> execution;
        if (parse.node().isPresent())
            execution = CompletableFuture.runAsync(() -> run(parse.node().get()), executorService);
        else {
            Exception ex = new Exception("Error parsing code: " + parse.errors().stream().map(x -> x.location() + ":" + x.message()).collect(Collectors.joining("\n")));
            execution = CompletableFuture.failedFuture(ex);
        }
        return execution.whenComplete((_, _) -> {
            if (debuggerCallback != null)
                debuggerCallback.accept(new ProgramState(0, Map.copyOf(stack)));
            state = State.STOPPED;
        });
    }

    private void run(Node node) {
        try {
            if (state != State.PAUSED && breakpoints.stream().anyMatch(x -> x == node.location().orElse(Location.EMPTY).startLine()) && !(node instanceof Node.CompundNode<?>)) {
                state = State.PAUSED;
            }

            if (state == State.PAUSED) {
                handleDebugCommand(node);
            }

            switch (node) {
                case Node.AlgoritimoNode algoritimoNode -> runAlgoritmo(algoritimoNode);
                case Node.ArrayTypeNode _ -> throw new UnsupportedOperationException("ArrayTypeNode not implemented");
                case Node.CommandNode commandNode -> runCommand(commandNode);
                case Node.CompundNode<?> compundNode -> compundNode.nodes().forEach(this::run);
                case Node.ConstantNode constantNode -> runConstant(constantNode);
                case Node.DosNode _ -> {}
                case Node.EmptyNode _ -> {}
                case Node.RegistroDeclarationNode _ -> throw new UnsupportedOperationException("RegistroDeclarationNode not implemented");
                case Node.SubprogramDeclarationNode subprogramDeclarationNode -> runSubprogramDeclaration(subprogramDeclarationNode);
                case Node.TypeNode _ -> throw new UnsupportedOperationException("TypeNode not implemented");
                case Node.VariableDeclarationNode variableDeclarationNode -> runVariableDeclaration(variableDeclarationNode);
                case Node.ExpressionNode e -> evaluate(e);
            }
        } catch (InterruptedException | IOException | BrokenBarrierException _) {
            state = State.STOPPED;
        }
    }

    CyclicBarrier lock = new CyclicBarrier(2);

    public void step() {
        try {
            if (state == State.PAUSED) {
                lock.await();
                state = State.PAUSED;
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }

    public void continueExecution() {
        try {
            if (state == State.PAUSED) {
                lock.await();
                state = State.RUNNING;
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleDebugCommand(Node node) throws BrokenBarrierException, InterruptedException {
        System.out.println("Breakpoint hit at line " + node.location().orElse(Location.EMPTY));
        if (debuggerCallback != null) {
            debuggerCallback.accept(new ProgramState(node.location().orElse(Location.EMPTY).startLine() - 1, Map.copyOf(stack)));
            lock.await();
            lock.reset();
        }
        else {
            continueExecution();
        }
    }

    private void runConstant(Node.ConstantNode constantNode) {
        stack.lastEntry().getValue().put(constantNode.name().id(), evaluate(constantNode.value()));
    }

    private void runSubprogramDeclaration(Node.SubprogramDeclarationNode subprogramDeclarationNode) {
        switch (subprogramDeclarationNode) {
            case Node.FunctionDeclarationNode functionDeclarationNode -> functions.put(functionDeclarationNode.name().id(), functionDeclarationNode);
            case Node.ProcedureDeclarationNode procedureDeclarationNode -> procedures.put(procedureDeclarationNode.name().id(), procedureDeclarationNode);
        }
    }

    private void runCommand(Node.CommandNode commandNode) throws IOException, InterruptedException, BrokenBarrierException {
        switch (commandNode) {
            case Node.AleatorioNode aleatorioNode -> runAleatorio(aleatorioNode);
            case Node.ArquivoCommandNode _ -> throw new UnsupportedOperationException("ArquivoCommandNode not implemented");
            case Node.AssignmentNode assignmentNode -> runAssignment(assignmentNode);
            case Node.ChooseCaseNode _ -> throw new UnsupportedOperationException("ChooseCaseNode not implemented");
            case Node.ChooseCommandNode chooseCommandNode -> {
                Node.ExpressionNode test = chooseCommandNode.expr();
                for (Node.ChooseCaseNode chooseCaseNode : chooseCommandNode.cases().nodes()) {
                    for (Node.ExpressionNode values : chooseCaseNode.value().nodes()) {
                         switch (values) {
                             case Node.RangeNode(Node.ExpressionNode start, Node.ExpressionNode end, _) -> {
                                 int value = ((Number) evaluate(test)).intValue();
                                 if (value >= ((Number) evaluate(start)).intValue() && value <= ((Number) evaluate(end)).intValue()) {
                                     run(chooseCaseNode.commands());
                                     return;
                                 }
                             }
                             case Node.ExpressionNode e -> {
                                 if ((Boolean) evaluate(new Node.EqNode(test, e, Optional.empty()))) {
                                     run(chooseCaseNode.commands());
                                     return;
                                 }
                             }
                         }
                     }
                }
                Node.ChooseCaseNode caseNode = Objects.requireNonNull(chooseCommandNode.defaultCase());
                var commands = caseNode.commands();
                run(commands);
            }
            case Node.ConditionalCommandNode conditionalCommandNode -> runConditionalCommand(conditionalCommandNode);
            case Node.CronometroCommandNode _ -> {}
            case Node.DebugCommandNode debugCommandNode -> runDebugCommand(debugCommandNode);
            case Node.EcoCommandNode ecoCommandNode -> eco = ecoCommandNode.on();
            case Node.ForCommandNode forCommandNode -> runForCommand(forCommandNode);
            case Node.IncrementNode _ -> throw new UnsupportedOperationException("IncrementNode not implemented");
            case Node.InterrompaCommandNode _ -> throw new BreakException();
            case Node.LimpatelaCommandNode _ -> {}
            case Node.PausaCommandNode _ -> {
                state = State.PAUSED;
                handleDebugCommand(commandNode);
            }
            case Node.ProcedureCallNode procedureCallNode -> {
                Node.ProcedureDeclarationNode procedureDeclaration = procedures.get(procedureCallNode.name().id());
                if (procedureDeclaration != null) {
                    callSubprogram(procedureCallNode, procedureDeclaration);
                } else if (procedureCallNode.name().id().equals("mudacor")) {
                } else {
                    throw new UnsupportedOperationException("Procedure not found: " + procedureCallNode.name().id());
                }

            }
            case Node.ReadCommandNode readCommandNode -> runReadCommand(readCommandNode);
            case Node.ReturnNode returnNode -> {
                stack.lastEntry().getValue().put("(RESULTADO)", evaluate(returnNode.expr()));
                throw new ReturnException();
            }
            case Node.TimerCommandNode _ -> {}
            case Node.WhileCommandNode whileCommandNode -> runWhileCommand(whileCommandNode);
            case Node.WriteCommandNode writeCommandNode -> runWriteCommandNode(writeCommandNode);
            case Node.WriteItemNode(Node.ExpressionNode expr, Node spaces, Node precision, _) -> runWriteItemNode(expr, spaces, precision);
        }
    }

    private void runAleatorio(Node.AleatorioNode aleatorioNode) {
        switch (aleatorioNode) {
            case Node.AleatorioOffNode _ -> aleatorio = AleatorioState.Off.INSTANCE;
            case Node.AleatorioOnNode _ -> aleatorio = new AleatorioState.Range(0, 100, 0);
            case Node.AleatorioRangeNode aleatorioRangeNode ->
                    aleatorio = new AleatorioState.Range(evaluate(aleatorioRangeNode.start()), evaluate(aleatorioRangeNode.end()), evaluate(aleatorioRangeNode.decimalPlaces()));
        }
    }

    private void runDebugCommand(Node.DebugCommandNode debugCommandNode) throws BrokenBarrierException, InterruptedException {
        if (evaluate(debugCommandNode.expr())) {
            state = State.PAUSED;
            handleDebugCommand(debugCommandNode);
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

    private void runAssignment(Node.AssignmentNode assignmentNode) {
        Object evaluate = evaluate(assignmentNode.expr());
        switch (assignmentNode.idOrArray()) {
            case Node.IdNode idNode -> assignVariable(idNode.id(), evaluate, AssignContext.SIMPLE);
            case Node.ArrayAccessNode arrayAccessNode -> {
                Node node = arrayAccessNode.node();
                Object o = evaluateVariableOrFunction(getIdentifierForArray(node));
                Node.CompundNode<Node.ExpressionNode> indexes = arrayAccessNode.indexes();
                switch(indexes.nodes().size()) {
                    case 1 -> {
                        int index = ((Number) evaluate(indexes.nodes().getFirst())).intValue();
                        if (o instanceof Double[] && evaluate instanceof Integer i)
                            evaluate = i.doubleValue();
                        Array.set(o, index, evaluate);
                    }
                    case 2 -> {
                        int index1 = ((Number) evaluate(indexes.nodes().getFirst())).intValue();
                        int index2 = ((Number) evaluate(indexes.nodes().getLast())).intValue();
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

    private void runWriteItemNode(Node.ExpressionNode expr, Node spaces, Node precision) {
        printValue(evaluate(expr), spaces, precision);
    }

    private void runWriteCommandNode(Node.WriteCommandNode writeCommandNode) {
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
        if (conditionalCommandNode instanceof Node.ConditionalCommandNode(Node.ExpressionNode expressionNode, Node.CompundNode<Node.CommandNode> command, Node.CompundNode<Node.CommandNode> elseCommand, _)) {
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
        boolean aleatorioEnabled = aleatorio != AleatorioState.Off.INSTANCE;
        readCommandNode.exprList().nodes().forEach(expr -> {
            switch (expr) {
                case Node.IdNode idNode -> {
                    Object o = evaluateVariableOrFunction(idNode);

                    final InputValue inputValue = aleatorioEnabled ? null : io.input().apply(new InputRequestValue(idNode.id(), InputRequestValue.Type.fromClass(o.getClass()))).join();
                    Object value = switch (o) {
                        case Boolean _ when aleatorioEnabled -> random.nextBoolean();
                        case String _ when aleatorioEnabled -> random.ints(65, 91)
                                .limit(5)
                                .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString();
                        case Double _ when aleatorio instanceof AleatorioState.Range range -> generateRandomDouble(range);
                        case Integer _ when aleatorio instanceof AleatorioState.Range(var start, var end, _) -> random.nextInt(start, end);

                        case Integer _ when inputValue instanceof InputValue.InteiroValue(var value1) -> value1;
                        case Double _ when inputValue instanceof InputValue.RealValue(var value1) -> value1;
                        case String _ when inputValue instanceof InputValue.CaracterValue(var value1) -> value1;
                        case Boolean _ when inputValue instanceof InputValue.LogicoValue(var value1) -> value1;

                        default -> throw new UnsupportedOperationException("Unsupported type: " + o.getClass());
                    };
                    if (eco)
                        io.output().accept(value + "\n");

                    assignVariable(idNode.id(), value, AssignContext.SIMPLE);
                }
                case Node.ArrayAccessNode arrayAccessNode -> {
                    Node.IdNode node = getIdentifierForArray(arrayAccessNode.node());
                    Object o = evaluateVariableOrFunction(node);

                    Node.CompundNode<Node.ExpressionNode> indexes = arrayAccessNode.indexes();
                    switch(indexes.nodes().size()) {
                        case 1 -> {
                            int index = ((Number) evaluate(indexes.nodes().getFirst())).intValue();
                            final InputValue inputValue = aleatorioEnabled ? null : io.input().apply(new InputRequestValue(node.id() + "[" + index + "]", InputRequestValue.Type.fromClass(o.getClass().getComponentType()))).join();
                            Array.set(o, index, readValueForArray(o, inputValue));
                        }
                        case 2 -> {
                            int index1 = ((Number) evaluate(indexes.nodes().getFirst())).intValue();
                            int index2 = ((Number) evaluate(indexes.nodes().getLast())).intValue();
                            final InputValue inputValue = aleatorioEnabled ? null : io.input().apply(new InputRequestValue(node.id() + "[" + index1 + "," + index2 + "]", InputRequestValue.Type.fromClass(o.getClass().getComponentType().getComponentType()))).join();
                            Array.set(Array.get(o, index1), index2, readValueForArray(Array.get(o, index1), inputValue));
                        }
                        default -> throw new UnsupportedOperationException("Unsupported number of indexes: " + indexes.nodes().size());
                    }
                }
                default -> throw new UnsupportedOperationException("Unsupported type: " + expr.getClass());
            }
        });

    }

    private static Node.IdNode getIdentifierForArray(Node arrayAccessNode) {
        return switch (arrayAccessNode) {
            case Node.ArrayAccessNode nestedAccess -> getIdentifierForArray(nestedAccess.node());
            case Node.IdNode idNode -> idNode;
            default -> throw new IllegalStateException("Unexpected value: " + arrayAccessNode);
        };
    }

    private Object readValueForArray(Object o, InputValue inputValue) {
        boolean aleatorioEnabled = aleatorio != AleatorioState.Off.INSTANCE;
        Object o1 = switch (o) {
            case Boolean[] _ when aleatorioEnabled -> random.nextBoolean();
            case String[] _ when aleatorioEnabled -> random.ints(65, 91)
                    .limit(5)
                    .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append);
            case Double[] _ when aleatorio instanceof AleatorioState.Range range -> generateRandomDouble(range);
            case Integer[] _ when aleatorio instanceof AleatorioState.Range(var start, var end, _) -> random.nextInt(start, end);

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

    private double generateRandomDouble(AleatorioState.Range range) {
        double v = random.nextDouble(range.start(), range.end());

        if (range.decimalPlaces == 0) {
            return (int) v;
        }
        BigDecimal bd = new BigDecimal(v);
        bd = bd.setScale(range.decimalPlaces, RoundingMode.HALF_UP);
        return bd.doubleValue();

    }

    private Object evaluateVariableOrFunction(Node.IdNode idNode) {
        return stack.reversed().values().stream().filter(m -> m.containsKey(idNode.id())).map(m -> m.get(idNode.id())).findFirst()
                .or(() -> Optional.ofNullable(functions.get(idNode.id())).map(_ -> new Node.FunctionCallNode(idNode, Node.CompundNode.empty(), Optional.empty())).map(this::evaluateFunction))
            .orElseThrow(() -> new UnsupportedOperationException("Variable not found: " + idNode.id()));
    }

    private void runForCommand(Node.ForCommandNode forCommandNode) {
        Node.IdNode identifier = forCommandNode.identifier();

        switch(forCommandNode) {
            case Node.ForCommandNode(_, _, Node.EmptyExpressionNode _, _, _, _) -> {
            }
            case Node.ForCommandNode(Node.IdNode id, Node.ExpressionNode start, Node.ExpressionNode end, Node.ExpressionNode step, Node.CompundNode<Node.CommandNode> command, _) -> {
                evaluateVariableOrFunction(identifier);
                int startValue = this.<Number>evaluate(start).intValue();
                int endValue = this.<Number>evaluate(end).intValue();
                int stepValue = this.<Number>evaluate(step).intValue();
                int i;
                if (stepValue < 0) {
                    for (i = startValue; i >= endValue; i += stepValue) {
                        assignVariable(id.id(), i, AssignContext.SIMPLE);
                        try {
                            run(command);
                        } catch (BreakException _) {
                        }
                    }
                } else {
                    for (i = startValue; i <= endValue; i += stepValue) {
                        assignVariable(id.id(), i, AssignContext.SIMPLE);
                        try {
                            run(command);
                        } catch (BreakException _) {
                        }
                    }
                }
                assignVariable(id.id(), i, AssignContext.SIMPLE);
            }

            default -> throw new UnsupportedOperationException("Unsupported type: " + forCommandNode.getClass());
        }
    }

    private void printValue(Object value, Node spaces, Node precision) {

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

    @SuppressWarnings("unchecked")
    private <T> T evaluate(Node.ExpressionNode node){
        return (T) switch (node) {
            case Node.StringLiteralNode(var value, _) -> value;
            case Node.BinaryNode binaryNode -> evaluateBinaryNode(binaryNode);
            case Node.BooleanLiteralNode(var value, _) -> value;
            case Node.FunctionCallNode functionCallNode -> evaluateFunction(functionCallNode);
            case Node.IntLiteralNode(var value, _) -> value;
            case Node.RealLiteralNode(var value, _) -> value;
            case Node.IdNode idNode -> evaluateVariableOrFunction(idNode);
            case Node.NegNode nedNode -> switch (evaluate(nedNode.expr())) {
                case Double x -> -x;
                case Integer x -> -x;
                case Object o -> throw new UnsupportedOperationException("unsupported neg node types: " + o);
            };
            case Node.PosNode(Node.ExpressionNode e, _) -> evaluate(e);
            case Node.NotNode notNode -> !(Boolean) evaluate(notNode.expr());
            case Node.EmptyExpressionNode _ -> 0;
            case Node.ArrayAccessNode arrayAccessNode -> evaluateArrayAccessNode(arrayAccessNode);
            case Node.MemberAccessNode _ -> throw new UnsupportedOperationException("MemberAccessNode not implemented");
            case Node.RangeNode _ -> throw new UnsupportedOperationException("RangeNode not implemented");
        };
    }

    private Object evaluateArrayAccessNode(Node.ArrayAccessNode arrayAccessNode) {
        Object o = evaluateVariableOrFunction(getIdentifierForArray(arrayAccessNode.node()));

        Node.CompundNode<Node.ExpressionNode> indexes = arrayAccessNode.indexes();
        switch(indexes.nodes().size()) {
            case 1 -> {
                int index = ((Number) evaluate(indexes.nodes().getFirst())).intValue();
                return Array.get(o, index);
            }
            case 2 -> {
                int index1 = ((Number) evaluate(indexes.nodes().getFirst())).intValue();
                int index2 = ((Number) evaluate(indexes.nodes().getLast())).intValue();
                return Array.get(Array.get(o, index1), index2);
            }
            default -> throw new UnsupportedOperationException("Unsupported number of indexes: " + indexes.nodes().size());
        }
    }

    private Object evaluateFunction(Node.FunctionCallNode functionCallNode) {
        Node.FunctionDeclarationNode functionDeclaration = functions.get(functionCallNode.name().id());
        if (functionDeclaration != null) {
            return callSubprogram(functionCallNode, functionDeclaration);
        } else if (StandardFunctions.FUNCTIONS.containsKey(functionCallNode.name().id())) {
            try {
                return StandardFunctions.FUNCTIONS.get(functionCallNode.name().id()).invokeWithArguments(functionCallNode.args().nodes().stream().map(this::evaluate).toList());
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        } else {
            throw new UnsupportedOperationException("Function not found: " + functionCallNode.name().id());
        }
    }

    private Object callSubprogram(Node.SubprogramCallNode subprogramCall, Node.SubprogramDeclarationNode subprogramDeclaration) {
        HashMap<String, Object> localVariables = new HashMap<>();
        String stackId = subprogramCall.name().id() + UUID.randomUUID();
        stack.putLast(stackId, localVariables);
        Node.CompundNode<Node.VariableDeclarationNode> parametersDeclaration = subprogramDeclaration.parameters();
        List<Node.VariableDeclarationNode> parameters = parametersDeclaration.nodes();
        List<Node.ExpressionNode> arguments = subprogramCall.args().nodes();
        if (parameters.size()  != arguments.size()) {
            throw new UnsupportedOperationException("Expected " + parameters.size() + " arguments but got " + arguments.size());
        }

        List<Object> argumentValues = arguments.stream().map(this::evaluate).toList();

        if (subprogramDeclaration instanceof Node.FunctionDeclarationNode functionDeclarationNode) {
            localVariables.put("(RESULTADO)", newInstance(functionDeclarationNode.returnType()));
        }

        run(parametersDeclaration);
        run(subprogramDeclaration.declarations());
        for (int i = 0; i < parameters.size(); i++){
            assignVariable(parameters.get(i).name().id(), argumentValues.get(i), AssignContext.ARGUMENT);
        }
        try {
            run(subprogramDeclaration.commands());
        } catch (ReturnException _) {
        }
        Object returnValue = switch (subprogramDeclaration) {
            case Node.FunctionDeclarationNode _ -> localVariables.get("(RESULTADO)");
            case Node.ProcedureDeclarationNode _ -> null;
        };

        List<Object> referenceValues = parameters.stream()
            .map(Node.VariableDeclarationNode::name)
            .map(Node.IdNode::id)
            .map(localVariables::remove)
            .toList();

        stack.remove(stackId);

        for (int i = 0; i < parameters.size(); i++) {
            if (!parameters.get(i).reference())
                continue;
            Node.ExpressionNode reference = arguments.get(i);
            Node.ExpressionNode newValue = valueToNode(referenceValues.get(i));
            runAssignment(new Node.AssignmentNode(reference, newValue, Optional.empty()));
        }
        return returnValue;
    }

    private static Node.ExpressionNode valueToNode(Object value) {
        return switch (value) {
            case String s -> new Node.StringLiteralNode(s, Optional.empty());
            case Double d -> new Node.RealLiteralNode(d, Optional.empty());
            case Integer d -> new Node.IntLiteralNode(d, Optional.empty());
            case Boolean d -> new Node.BooleanLiteralNode(d, Optional.empty());
            default -> throw new UnsupportedOperationException("Unsupported type: " + value);
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
                    case PairValue(String x, String y) -> x + y;
                    case Object o -> throw new UnsupportedOperationException("unsupported add node types: " + o);
                };
            }
            case Node.DivNode(Node.ExpressionNode left, Node.ExpressionNode right, boolean integerResult, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                Number result = switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Number x, Double y) -> x.doubleValue() / y;
                    case PairValue(Double x, Number y) -> x / y.doubleValue();

                    case PairValue(Number x, Number y) -> x.intValue() / y.intValue();
                    case Object o -> throw new UnsupportedOperationException("unsupported div node types: " + o);
                };
                yield integerResult ? result.intValue() : result;
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

    private void runAlgoritmo(Node.AlgoritimoNode algoritimoNode) throws InterruptedException {
        stack.putLast("GLOBAL", new HashMap<>());
        run(algoritimoNode.declarations());
        run(algoritimoNode.commands());
    }

    private void runVariableDeclaration(Node.VariableDeclarationNode variableDeclarationNode) {
        stack.lastEntry().getValue().put(variableDeclarationNode.name().id(), newInstance(variableDeclarationNode.type()));
    }

    enum AssignContext {
        SIMPLE,
        ARGUMENT
    }

    private void assignVariable(String name, Object value, AssignContext context) {
        stack.reversed().values().stream().filter(m -> m.containsKey(name)).findFirst().ifPresentOrElse(m -> {
            Object valueToAssign = value;
            switch (context) {
                case ARGUMENT -> {
                    Class<?> variableClass = m.get(name).getClass();
                    Class<?> valueClass = value.getClass();
                    if (variableClass == Integer.class && valueClass == Double.class) {
                        valueToAssign = ((Number) value).intValue();
                    }
                }
                case SIMPLE -> {
                    Class<?> variableClass = m.get(name).getClass();
                    Class<?> valueClass = value.getClass();
                    if (variableClass == Integer.class && valueClass == Double.class) {
                        throw new UnsupportedOperationException("Cannot assign " + valueClass + " to " + variableClass);
                    }
                }
            }
            m.put(name, valueToAssign);
        }, () -> {
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
            case Node.ArrayTypeNode(Node.TypeNode type, Node.CompundNode<Node> sizes, _) -> {
                Class<?> typeClass = getType(type);
                int[] dimensions = sizes.nodes().stream()
                        .map(Node.RangeNode.class::cast)
                        .mapToInt(node -> (Integer)evaluate(node.end()) + 2)
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
