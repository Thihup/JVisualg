package dev.thihup.jvisualg.interpreter;

import dev.thihup.jvisualg.frontend.ASTResult;
import dev.thihup.jvisualg.frontend.VisualgParser;
import dev.thihup.jvisualg.frontend.node.Location;
import dev.thihup.jvisualg.frontend.node.Node;
import dev.thihup.jvisualg.interpreter.TypeException.InvalidOperand.Operator;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

@NullMarked
public class Interpreter {

    public final SequencedMap<String, Map<String, Object>> stack = new LinkedHashMap<>();
    private final Map<String, Node.FunctionDeclarationNode> functions = new LinkedHashMap<>();
    private final Map<String, Node.ProcedureDeclarationNode> procedures = new LinkedHashMap<>();
    private final Map<String, UserDefinedType> userDefinedTypeMap = new LinkedHashMap<>();
    private static final RandomGenerator random = RandomGenerator.getDefault();
    private final IO io;
    @Nullable
    private final Consumer<ProgramState> debuggerCallback;

    private AleatorioState aleatorio = AleatorioState.Off.INSTANCE;
    private boolean eco = false;
    private List<Integer> breakpoints = new ArrayList<>();

    public sealed interface State {
        enum NotStarted implements State {INSTANCE}

        enum Running implements State {INSTANCE}

        record PausedDebug(int lineNumber) implements State {
        }

        enum CompletedSuccessfully implements State {INSTANCE}

        record CompletedExceptionally(Throwable throwable) implements State {
        }

        enum ForcedStop implements State {INSTANCE}
    }
    private volatile State state = State.NotStarted.INSTANCE;

    private sealed interface AleatorioState {

        default InputValue generateValue(InputRequestValue requestValue) {
            throw new UnsupportedOperationException();
        }

        record Range(int start, int end, int decimalPlaces) implements AleatorioState {
            @Override
            public InputValue generateValue(InputRequestValue requestValue) {
                return switch (requestValue.type()) {
                    case CARACTER -> new InputValue.CaracterValue(random.ints(65, 91)
                            .limit(5)
                            .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append).toString());
                    case LOGICO -> new InputValue.LogicoValue(random.nextBoolean());
                    case REAL -> new InputValue.RealValue(generateRandomDouble(this));
                    case INTEIRO -> new InputValue.InteiroValue(random.nextInt(start, end));
                };

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
        }
        enum Off implements AleatorioState {
            INSTANCE
        }
    }


    public record ProgramState(int lineNumber, Map<String, Map<String, Object>> stack) {
    }

    public Interpreter(IO io, @Nullable Consumer<ProgramState> debuggerCallback) {
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
        state = State.NotStarted.INSTANCE;
    }

    public State state() {
        return state;
    }

    public CompletableFuture<Void> run(String code, ExecutorService executorService) {
        return CompletableFuture.runAsync(() -> {
            state = State.Running.INSTANCE;
            ASTResult parse = VisualgParser.parse(code);
                parse.node()
                    .ifPresentOrElse(this::run, () -> {
                        throw new RuntimeException("Error parsing code: " + parse.errors().stream().map(x -> x.location() + ":" + x.message()).collect(Collectors.joining("\n")));
                    });

        }, executorService).whenComplete((_, exception) -> {
            if (debuggerCallback != null) {
                debuggerCallback.accept(new ProgramState(0, stack));
            }
            state = exception != null ? new State.CompletedExceptionally(exception) : State.CompletedSuccessfully.INSTANCE;
        });
    }

    private void run(Node node) {
        try {
            switch (state) {
                case State.ForcedStop _ -> throw new CancellationException("Program was cancelled");
                case State.CompletedSuccessfully _ -> {
                    return;
                }
                case State.PausedDebug _ -> handleDebugCommand(node);

                case State.CompletedExceptionally _, State.Running _, State.NotStarted _ -> {
                }

            }

            int lineNumber = node.location().orElse(Location.EMPTY).startLine();
            if (!(state instanceof State.PausedDebug) && breakpoints.contains(lineNumber) &&
                !(node instanceof Node.CompundNode<?>)) {
                state = new State.PausedDebug(lineNumber);
                handleDebugCommand(node);
            }

            switch (node) {
                case Node.AlgoritimoNode algoritimoNode -> runAlgoritmo(algoritimoNode);
                case Node.CommandNode commandNode -> runCommand(commandNode);
                case Node.CompundNode<?> compundNode -> runCompundNode(compundNode);
                case Node.DosNode _, Node.EmptyNode _ -> {}
                case Node.DeclarationNode declarationNode -> runDeclaration(declarationNode);
                case Node.TypeNode _ -> throw new UnsupportedOperationException("TypeNode not implemented");
                case Node.ExpressionNode e -> evaluate(e);
            }
        } catch (IOException | InterruptedException | BrokenBarrierException e) {
            state = new State.CompletedExceptionally(e);
        } catch (StopExecutionException _) {
            state = State.CompletedSuccessfully.INSTANCE;
        }
    }

    private void runCompundNode(Node.CompundNode<?> compundNode) {
        compundNode.nodes().forEach(this::run);
    }

    public void stop() {
        state = State.ForcedStop.INSTANCE;
    }

    private void runDeclaration(Node.DeclarationNode declarationNode) {
        switch (declarationNode) {
            case Node.ConstantNode constantNode -> runConstant(constantNode);
            case Node.RegistroDeclarationNode registroDeclarationNode -> runRegistroDeclaration(registroDeclarationNode);
            case Node.SubprogramDeclarationNode subprogramDeclarationNode -> runSubprogramDeclaration(subprogramDeclarationNode);
            case Node.VariableDeclarationNode variableDeclarationNode -> runVariableDeclaration(variableDeclarationNode);
        }
    }

    private void runRegistroDeclaration(Node.RegistroDeclarationNode registroDeclarationNode) {
        Node.IdNode name = registroDeclarationNode.name();

        Map<String, Node.TypeNode> fields = registroDeclarationNode.variableDeclarationContexts().nodes().stream()
            .collect(Collectors.toMap(x -> x.name().id(), Node.VariableDeclarationNode::type));

        userDefinedTypeMap.put(name.id(), new UserDefinedType(name.id(), fields));
    }

    CyclicBarrier lock = new CyclicBarrier(2);

    public void step() {
        try {
            if (state instanceof State.PausedDebug) {
                if (lock.getNumberWaiting() == 1) {
                    lock.await();
                }
//                state = State.PAUSED_DEBUG;
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }

    public void continueExecution() {
        try {
            if (state instanceof State.PausedDebug) {
                if (lock.getNumberWaiting() == 1) {
                    lock.await();
                }
                state = State.Running.INSTANCE;
            }
        } catch (InterruptedException | BrokenBarrierException e) {
            throw new RuntimeException(e);
        }
    }

    private void handleDebugCommand(Node node) throws BrokenBarrierException, InterruptedException {
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
                                 if (evaluate(new Node.EqNode(test, e, Optional.empty()))) {
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
            case Node.EndAlgorithmCommand _ -> throw new StopExecutionException();
            case Node.EcoCommandNode ecoCommandNode -> eco = ecoCommandNode.on();
            case Node.ForCommandNode forCommandNode -> runForCommand(forCommandNode);
            case Node.InterrompaCommandNode _ -> throw new BreakException();
            case Node.LimpatelaCommandNode _ -> io.output().accept(new OutputEvent.Clear());
            case Node.PausaCommandNode _ -> {
                state = new State.PausedDebug(commandNode.location().orElse(Location.EMPTY).startLine());
                handleDebugCommand(commandNode);
            }
            case Node.ProcedureCallNode procedureCallNode -> {
                Node.ProcedureDeclarationNode procedureDeclaration = procedures.get(procedureCallNode.name().id());
                if (procedureDeclaration != null) {
                    callSubprogram(procedureCallNode, procedureDeclaration);
                } else if (procedureCallNode.name().id().equals("mudacor")) {
                    if (procedureCallNode.args().nodes().size() != 2) {
                        throw new TypeException.WrongNumberOfArguments(2, procedureCallNode.args().nodes().size());
                    }
                    try {
                        OutputEvent.ChangeColor.Color color = OutputEvent.ChangeColor.Color.fromString(evaluate(procedureCallNode.args().nodes().getFirst()));
                        OutputEvent.ChangeColor.Position position = OutputEvent.ChangeColor.Position.fromString(evaluate(procedureCallNode.args().nodes().getLast()));
                        io.output().accept(new OutputEvent.ChangeColor(color, position));
                    } catch (IllegalArgumentException _) {

                    }
                } else {
                    throw new TypeException.ProcedureNotFound(procedureCallNode.name().id());
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
            state = new State.PausedDebug(debugCommandNode.location().orElse(Location.EMPTY).startLine());
            handleDebugCommand(debugCommandNode);
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
                        if (o.getClass().getComponentType() != evaluate.getClass()) {
                            if (o instanceof Double[] && evaluate instanceof Integer i)
                                evaluate = i.doubleValue();
                            else
                                throw new TypeException.InvalidAssignment(o.getClass().getComponentType(), evaluate.getClass());
                        }
                        Array.set(o, index, evaluate);
                    }
                    case 2 -> {
                        int index1 = ((Number) evaluate(indexes.nodes().getFirst())).intValue();
                        int index2 = ((Number) evaluate(indexes.nodes().getLast())).intValue();
                        Object array = Array.get(o, index1);
                        if (array.getClass().getComponentType() != evaluate.getClass()) {
                            if (o instanceof Double[] && evaluate instanceof Integer i)
                                evaluate = i.doubleValue();
                            else
                                throw new TypeException.InvalidAssignment(array.getClass().getComponentType(), evaluate.getClass());
                        }

                        Array.set(array, index2, evaluate);
                    }
                    default -> throw new TypeException.InvalidIndex(indexes.nodes().size());
                }

            }
            case Node.MemberAccessNode memberAccessNode -> {
                Object evaluateMember = evaluate(memberAccessNode.node());
                if (!(evaluateMember instanceof UserDefinedValue userDefinedValue)) {
                    throw unsupportedType(evaluate);
                }
                if (!(memberAccessNode.member() instanceof Node.IdNode idNode)) {
                    throw unsupportedType(memberAccessNode.member());
                }
                UserDefinedType userDefinedType = userDefinedValue.type();
                Node.TypeNode typeNode = userDefinedType.fields().get(idNode.id());
                if (typeNode == null) {
                    throw new TypeException.VariableNotFound(idNode.id());
                }

                Class<?> variableClass = getType(userDefinedValue.type().fields().get(idNode.id()));
                Class<?> valueClass = evaluate.getClass();
                Object valueToAssign = evaluate;
                if (variableClass != valueClass) {
                    if (variableClass == Double.class && valueClass == Integer.class) {
                        valueToAssign = ((Number) evaluate).doubleValue();
                    } else {
                        throw new TypeException.InvalidAssignment(variableClass, valueClass);
                    }
                }

                userDefinedValue.values().put(idNode.id(), valueToAssign);
            }
            case null, default -> throw unsupportedType(assignmentNode);
        }
    }

    private void runWriteItemNode(Node.ExpressionNode expr, Node spaces, Node precision) {
        printValue(evaluate(expr), spaces, precision);
    }

    private void runWriteCommandNode(Node.WriteCommandNode writeCommandNode) {
        run(writeCommandNode.writeList());
        if (writeCommandNode.newLine()) {
            io.output().accept(new OutputEvent.Text("\n"));
        }
    }

    private void runWhileCommand(Node.WhileCommandNode whileCommandNode) {
        try {
            if (whileCommandNode.conditionAtEnd()) {
                do {
                    run(whileCommandNode.commands());
                } while (!(Boolean) evaluate(whileCommandNode.test()));
            } else {
                while (evaluate(whileCommandNode.test())) {
                    run(whileCommandNode.commands());
                }
            }
        } catch (BreakException _) {
        }
    }

    private void runConditionalCommand(Node.ConditionalCommandNode conditionalCommandNode) {
        if (evaluate(conditionalCommandNode.expr())) {
            run(conditionalCommandNode.commands());
        } else {
            run(conditionalCommandNode.elseCommands());
        }
    }

    private void runReadCommand(Node.ReadCommandNode readCommandNode) {
        readCommandNode.exprList().nodes().forEach(expr -> {
            switch (expr) {
                case Node.IdNode idNode -> {
                    Object o = evaluateVariableOrFunction(idNode);
                    InputRequestValue inputRequest = new InputRequestValue(idNode.id(), InputRequestValue.Type.fromClass(o.getClass()));
                    Object value = readValue(inputRequest, o);
                    assignVariable(idNode.id(), value, AssignContext.SIMPLE);
                }
                case Node.ArrayAccessNode arrayAccessNode -> {
                    Node.IdNode node = getIdentifierForArray(arrayAccessNode.node());
                    Object o = evaluateVariableOrFunction(node);

                    Node.CompundNode<Node.ExpressionNode> indexes = arrayAccessNode.indexes();
                    switch(o) {
                        case Object[][] multiarray -> {
                            int index1 = ((Number) evaluate(indexes.nodes().getFirst())).intValue();
                            int index2 = ((Number) evaluate(indexes.nodes().getLast())).intValue();
                            InputRequestValue inputRequest = new InputRequestValue(node.id() + "[" + index1 + "," + index2 + "]", InputRequestValue.Type.fromClass(o.getClass().getComponentType().getComponentType()));
                            multiarray[index1][index2] = readValue(inputRequest, multiarray[index1][index2]);
                        }
                        case Object[] array -> {
                            int index = ((Number) evaluate(indexes.nodes().getFirst())).intValue();
                            InputRequestValue inputRequest = new InputRequestValue(node.id() + "[" + index + "]", InputRequestValue.Type.fromClass(o.getClass().getComponentType()));
                            array[index] = readValue(inputRequest, array[index]);
                        }
                        default -> throw new TypeException.InvalidIndex(indexes.nodes().size());
                    }
                }
                case Node.MemberAccessNode(Node.ExpressionNode node, Node member, _) -> {
                    Object evaluate = evaluate(node);
                    if (!(evaluate instanceof UserDefinedValue userDefinedValue)) {
                        throw unsupportedType(evaluate);
                    }
                    if (!(member instanceof Node.IdNode idNode)) {
                        throw unsupportedType(member);
                    }
                    UserDefinedType userDefinedType = userDefinedValue.type();
                    Node.TypeNode typeNode = userDefinedType.fields().get(idNode.id());
                    if (typeNode == null) {
                        throw new TypeException.VariableNotFound(idNode.id());
                    }
                    Class<?> type = getType(typeNode);
                    InputRequestValue inputRequest = new InputRequestValue(idNode.id(), InputRequestValue.Type.fromClass(type));
                    Object value = readValue(inputRequest, userDefinedValue.values().get(idNode.id()));
                    userDefinedValue.values().put(idNode.id(), value);
                }
                default -> throw unsupportedType(expr);
            }
        });

    }

    private Object readValue(InputRequestValue inputRequest, Object o) {
        boolean aleatorioEnabled = aleatorio != AleatorioState.Off.INSTANCE;
        InputValue inputValue = aleatorioEnabled ? aleatorio.generateValue(inputRequest) : io.input().apply(inputRequest).join();
        Object value = switch (o) {
            case Integer _ when inputValue instanceof InputValue.InteiroValue(var value1) -> value1;
            case Double _ when inputValue instanceof InputValue.RealValue(var value1) -> value1;
            case String _ when inputValue instanceof InputValue.CaracterValue(var value1) -> value1;
            case Boolean _ when inputValue instanceof InputValue.LogicoValue(var value1) -> value1;
            default -> throw new TypeException.MissingInput(inputRequest);
        };
        if (eco && aleatorioEnabled)
            io.output().accept(new OutputEvent.Text(value + "\n"));
        return value;
    }

    private static Node.IdNode getIdentifierForArray(Node arrayAccessNode) {
        return switch (arrayAccessNode) {
            case Node.ArrayAccessNode nestedAccess -> getIdentifierForArray(nestedAccess.node());
            case Node.IdNode idNode -> idNode;
            default -> throw new UnsupportedOperationException("Unexpected value: " + arrayAccessNode);
        };
    }

    private Object evaluateVariableOrFunction(Node.IdNode idNode) {
        return stack.reversed().values().stream().filter(m -> m.containsKey(idNode.id())).map(m -> m.get(idNode.id())).findFirst()
                .or(() -> Optional.ofNullable(functions.get(idNode.id())).map(_ -> new Node.FunctionCallNode(idNode, Node.CompundNode.empty(), Optional.empty())).map(this::evaluateFunction))
                .or(() -> Optional.ofNullable(StandardFunctions.FUNCTIONS.get(idNode.id())).map(_ -> new Node.FunctionCallNode(idNode, Node.CompundNode.empty(), Optional.empty())).map(this::evaluateFunction))
            .orElseThrow(() -> new TypeException.VariableNotFound(idNode.id()));
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

            default -> throw unsupportedType(forCommandNode);
        }
    }

    private void printValue(Object value, Node spaces, Node precision) {

        if (value instanceof Integer i) {
            value = i.doubleValue();
        }

        int spacesValue = switch (spaces) {
            case Node.ExpressionNode e when evaluate(e) instanceof Number p -> p.intValue();
            case Node.EmptyNode _ -> 0;
            default -> throw unsupportedType(spaces);
        };

        int precisionValue = switch (precision) {
            case Node.ExpressionNode e when evaluate(e) instanceof Number p -> p.intValue();
            case Node.EmptyNode _ -> 0;
            default -> throw unsupportedType(precision);
        };

        NumberFormat numberFormat = NumberFormat.getNumberInstance(Locale.US);
        numberFormat.setRoundingMode(RoundingMode.HALF_UP);
        numberFormat.setMaximumFractionDigits(0);
        numberFormat.setMinimumFractionDigits(0);
        String text = switch (value) {
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
            case null, default -> throw unsupportedType(value);
        };
        io.output().accept(new OutputEvent.Text(text));

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
            case Node.NegNode nedNode -> evaluateNegNode(nedNode);
            case Node.PosNode(Node.ExpressionNode e, _) -> evaluate(e);
            case Node.NotNode notNode -> evaluateNotNode(notNode);
            case Node.EmptyExpressionNode _ -> 0;
            case Node.ArrayAccessNode arrayAccessNode -> evaluateArrayAccessNode(arrayAccessNode);
            case Node.MemberAccessNode memberAccessNode -> evaluateMemberAccessNode(memberAccessNode);
            case Node.RangeNode _ -> throw new UnsupportedOperationException("RangeNode not implemented");
        };
    }

    private Object evaluateMemberAccessNode(Node.MemberAccessNode memberAccessNode) {
        Object evaluate = evaluate(memberAccessNode.node());
        if (!(evaluate instanceof UserDefinedValue userDefinedValue)) {
            throw new UnsupportedOperationException("Unsupported type: " + evaluate.getClass());
        }
        Node.IdNode member = (Node.IdNode) memberAccessNode.member();
        UserDefinedType userDefinedType = userDefinedValue.type();
        Node.TypeNode typeNode = userDefinedType.fields().get(member.id());
        if (typeNode == null) {
            throw new TypeException.VariableNotFound(member.id());
        }
        return userDefinedValue.values().get(member.id());
    }

    private Object evaluateNotNode(Node.NotNode notNode) {
        return switch (evaluate(notNode.expr())) {
            case Boolean b -> !b;
            case null, default -> throw new TypeException.InvalidOperand(Operator.NOT, notNode.expr().getClass());
        };
    }
    private static UnsupportedOperationException unsupportedType(Object evaluate) {
        return new UnsupportedOperationException("Unsupported type: " + evaluate);
    }
    private Object evaluateNegNode(Node.NegNode nedNode) {
        return switch (evaluate(nedNode.expr())) {
            case Double d -> -d;
            case Integer i -> -i;
            case null, default -> throw unsupportedType(nedNode.expr());
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
            default -> throw new TypeException.InvalidIndex(indexes.nodes().size());
        }
    }

    private Object evaluateFunction(Node.FunctionCallNode functionCallNode) {
        Node.FunctionDeclarationNode functionDeclaration = functions.get(functionCallNode.name().id());
        if (functionDeclaration != null) {
            return Objects.requireNonNull(callSubprogram(functionCallNode, functionDeclaration));
        } else if (StandardFunctions.FUNCTIONS.containsKey(functionCallNode.name().id())) {
            MethodHandle methodHandle = StandardFunctions.FUNCTIONS.get(functionCallNode.name().id());
            List<Object> list = functionCallNode.args().nodes().stream().map(this::evaluate).toList();
            if (methodHandle.type().parameterCount() != list.size()) {
                throw new TypeException.WrongNumberOfArguments(methodHandle.type().parameterCount(), list.size());
            }
            try {
                return methodHandle.invokeWithArguments(list);
            } catch (Throwable e) {
                throw new UnsupportedOperationException(e);
            }
        } else {
            throw new TypeException.FunctionNotFound(functionCallNode.name().id());
        }
    }

    @Nullable
    private Object callSubprogram(Node.SubprogramCallNode subprogramCall, Node.SubprogramDeclarationNode subprogramDeclaration) {
        HashMap<String, Object> localVariables = new HashMap<>();
        String stackId = subprogramCall.name().id() + UUID.randomUUID();
        stack.putLast(stackId, localVariables);
        Node.CompundNode<Node.VariableDeclarationNode> parametersDeclaration = subprogramDeclaration.parameters();
        List<Node.VariableDeclarationNode> parameters = parametersDeclaration.nodes();
        List<Node.ExpressionNode> arguments = subprogramCall.args().nodes();
        if (parameters.size() != arguments.size()) {
            throw new TypeException.WrongNumberOfArguments(parameters.size(), arguments.size());
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
            if (!parameters.get(i).reference()) {
                continue;
            }
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
            default -> throw unsupportedType(value);
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
                    case Object _ -> throw new TypeException.InvalidOperand(Operator.ADD, leftResult.getClass(), rightResult.getClass());
                };
            }
            case Node.DivNode(Node.ExpressionNode left, Node.ExpressionNode right, boolean integerResult, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                Number result = switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Number x, Double y) -> x.doubleValue() / y;
                    case PairValue(Double x, Number y) -> x / y.doubleValue();

                    case PairValue(Number x, Number y) -> x.intValue() / y.intValue();
                    case Object _ -> throw new TypeException.InvalidOperand(Operator.DIVIDE, leftResult.getClass(), rightResult.getClass());
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
                    case Object _ ->
                            throw new TypeException.InvalidOperand(Operator.MODULO, leftResult.getClass(), rightResult.getClass());
                };
            }
            case Node.MulNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Number x, Double y) -> x.doubleValue() * y;
                    case PairValue(Double x, Number y) -> x * y.doubleValue();

                    case PairValue(Number x, Number y) -> x.intValue() * y.intValue();
                    case Object _ -> throw new TypeException.InvalidOperand(Operator.MULTIPLY, leftResult.getClass(), rightResult.getClass());
                };
            }

            case Node.PowNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Number x, Double y) -> Math.pow(x.doubleValue(),y);
                    case PairValue(Double x, Number y) -> Math.pow(x, y.doubleValue());

                    case PairValue(Number x, Number y) -> (int) Math.pow(x.intValue(), y.intValue());
                    case Object _ -> throw new TypeException.InvalidOperand(Operator.POW, leftResult.getClass(), rightResult.getClass());
                };
            }
            case Node.SubNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Number x, Double y) -> x.doubleValue() - y;
                    case PairValue(Double x, Number y) -> x - y.doubleValue();

                    case PairValue(Number x, Number y) -> x.intValue() - y.intValue();
                    case Object _ -> throw new TypeException.InvalidOperand(Operator.SUBTRACT, leftResult.getClass(), rightResult.getClass());
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
                    case Object _ -> throw new TypeException.InvalidOperand(Operator.AND, leftResult.getClass(), rightResult.getClass());
                };
            }
            case Node.OrNode(Node.ExpressionNode left, Node.ExpressionNode right, _) -> {
                Object leftResult = evaluate(left);
                Object rightResult = evaluate(right);

                yield switch (new PairValue(leftResult, rightResult)) {
                    case PairValue(Boolean x, Boolean y) -> x || y;
                    case Object _ -> throw new TypeException.InvalidOperand(Operator.OR, leftResult.getClass(), rightResult.getClass());
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
                    case Object _ ->
                            throw new TypeException.InvalidOperand(Operator.GREATER_THAN_OR_EQUALS, leftResult.getClass(), rightResult.getClass());
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
                    case Object _ ->
                            throw new TypeException.InvalidOperand(Operator.GREATER_THAN, leftResult.getClass(), rightResult.getClass());
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
                    case Object _ -> throw new TypeException.InvalidOperand(Operator.LESS_THAN_OR_EQUALS, leftResult.getClass(), rightResult.getClass());
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
                    case Object _ -> throw new TypeException.InvalidOperand(Operator.LESS_THAN, leftResult.getClass(), rightResult.getClass());
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
                    case Object _ -> throw new TypeException.InvalidOperand(Operator.EQUALS, leftResult.getClass(), rightResult.getClass());
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
                    case Object _ -> throw new TypeException.InvalidOperand(Operator.NOT_EQUALS, leftResult.getClass(), rightResult.getClass());
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
                    if (valueClass != variableClass) {
                        if (variableClass == Integer.class && valueClass == Double.class) {
                            valueToAssign = ((Number) value).intValue();
                        } else if (variableClass == Double.class && valueClass == Integer.class) {
                            valueToAssign = ((Number) value).doubleValue();
                        } else {
                            throw new TypeException.InvalidAssignment(variableClass, valueClass);
                        }
                    }
                }
                case SIMPLE -> {
                    Class<?> variableClass = m.get(name).getClass();
                    Class<?> valueClass = value.getClass();
                    if (variableClass != valueClass) {
                        if (variableClass == Double.class && valueClass == Integer.class) {
                            valueToAssign = ((Number) value).doubleValue();
                        } else {
                            throw new TypeException.InvalidAssignment(variableClass, valueClass);
                        }
                    }
                }
            }
            m.put(name, valueToAssign);
        }, () -> {
            throw new TypeException.VariableNotFound(name);
        });
    }

    private Class<?> getType(Node typeNode) {
        return switch (typeNode) {
            case Node.InteiroType _ -> Integer.class;
            case Node.RealType _ -> Double.class;
            case Node.CaracterType _ -> String.class;
            case Node.LogicoType _ -> Boolean.class;
            case Node.UserDefinedType(Node.StringLiteralNode(var type, _), _) -> switch (type.toLowerCase()) {
                    case String s -> {
                    if (!userDefinedTypeMap.containsKey(s)) {
                        throw new TypeException.TypeNotFound(s);
                    }
                    yield UserDefinedValue.class;
                }
            };
            default -> throw new UnsupportedOperationException("Unexpected value: " + typeNode);
        };
    }

    private Object newInstance(Node.TypeNode typeNode) {
        return switch (typeNode) {

            case Node.InteiroType _ -> 0;
            case Node.RealType _ -> 0.0;
            case Node.CaracterType _ -> "";
            case Node.LogicoType _ -> false;
            case Node.UserDefinedType(Node.StringLiteralNode(var type, _), _) -> switch (type.toLowerCase()) {
                case String s -> {
                    if (!userDefinedTypeMap.containsKey(s)) {
                        throw new TypeException.TypeNotFound(s);
                    }
                    UserDefinedType userDefinedType = userDefinedTypeMap.get(s);
                    Map<String, Node.TypeNode> fields = userDefinedType.fields();
                    Map<String, Object> collect = fields.entrySet().stream()
                            .map(e -> new AbstractMap.SimpleEntry<>(e.getKey(), newInstance(e.getValue())))
                            .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                    yield new UserDefinedValue(userDefinedType, collect);
                }
            };
            case Node.ArrayTypeNode(Node.TypeNode type, Node.CompundNode<Node.RangeNode> sizes, _) -> {
                Class<?> typeClass = getType(type);
                int[] dimensions = sizes.nodes().stream()
                        .mapToInt(node -> (Integer) evaluate(node.end()) + 2)
                        .toArray();

                Object o = Array.newInstance(typeClass, dimensions);
                switch (o) {
                    case Object[][] multiArray ->
                            Arrays.stream(multiArray).forEach(x -> Arrays.setAll(x, _ -> newInstance(type)));
                    case Object[] array -> Arrays.setAll(array, _ -> newInstance(type));
                    default -> throw new UnsupportedOperationException("Unexpected value: " + o);
                }
                yield o;
            }
        };
    }
}
