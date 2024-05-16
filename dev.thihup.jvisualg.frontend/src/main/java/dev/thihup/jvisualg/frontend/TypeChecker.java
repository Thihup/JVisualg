package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.frontend.node.Location;
import dev.thihup.jvisualg.frontend.node.Node;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.thihup.jvisualg.frontend.TypeChecker.Type.PrimitiveTypes.*;

public class TypeChecker {

    public static final Scope DEFAULT_GLOBAL_SCOPE = new Scope("DEFAULT", Map.of(), Map.of(),
            Map.ofEntries(
                    Map.entry("abs", new Declaration.Function("abs", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),
                    Map.entry("arccos", new Declaration.Function("arccos", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),
                    Map.entry("arcsen", new Declaration.Function("arcsen", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),
                    Map.entry("arctan", new Declaration.Function("arctan", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),
                    Map.entry("asc", new Declaration.Function("asc", Type.PrimitiveTypes.INTEIRO, Map.of("s", new Declaration.Variable("s", CARACTERE)))),


                    Map.entry("carac", new Declaration.Function("carac", CARACTERE, Map.of("c", new Declaration.Variable("c", Type.PrimitiveTypes.INTEIRO)))),
                    Map.entry("caracpnum", new Declaration.Function("caracpnum", Type.PrimitiveTypes.INTEIRO, Map.of("c", new Declaration.Variable("c", CARACTERE)))),
                    Map.entry("compr", new Declaration.Function("compr", Type.PrimitiveTypes.INTEIRO, Map.of("c", new Declaration.Variable("c", CARACTERE)))),

                    Map.entry("copia", new Declaration.Function("copia", CARACTERE, Map.of("c", new Declaration.Variable("c", CARACTERE), "p", new Declaration.Variable("p", Type.PrimitiveTypes.INTEIRO), "n", new Declaration.Variable("n", Type.PrimitiveTypes.INTEIRO)))),
                    Map.entry("cos", new Declaration.Function("cos", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),
                    Map.entry("cotan", new Declaration.Function("cotan", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),

                    Map.entry("exp", new Declaration.Function("exp", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),

                    Map.entry("grauprad", new Declaration.Function("grauprad", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),

                    Map.entry("int", new Declaration.Function("int", Type.PrimitiveTypes.INTEIRO, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),

                    Map.entry("log", new Declaration.Function("log", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),
                    Map.entry("logn", new Declaration.Function("logn", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),

                    Map.entry("maiusc", new Declaration.Function("maiusc", CARACTERE, Map.of("c", new Declaration.Variable("c", CARACTERE)))),
                    Map.entry("minusc", new Declaration.Function("minusc", CARACTERE, Map.of("c", new Declaration.Variable("c", CARACTERE)))),

                    Map.entry("numpcarac", new Declaration.Function("numpcarac", CARACTERE, Map.of("n", new Declaration.Variable("n", Type.PrimitiveTypes.REAL)))),


                    Map.entry("pos", new Declaration.Function("pos", Type.PrimitiveTypes.INTEIRO, Map.of("subc", new Declaration.Variable("subc", CARACTERE), "c", new Declaration.Variable("c", CARACTERE)))),
                    Map.entry("pi", new Declaration.Function("pi", Type.PrimitiveTypes.REAL, Map.of())),


                    Map.entry("quad", new Declaration.Function("quad", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),
                    Map.entry("radpgrau", new Declaration.Function("radpgrau", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),
                    Map.entry("raizq", new Declaration.Function("raizq", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),
                    Map.entry("rand", new Declaration.Function("rand", Type.PrimitiveTypes.REAL, Map.of())),
                    Map.entry("randi", new Declaration.Function("randi", Type.PrimitiveTypes.INTEIRO, Map.of("limite", new Declaration.Variable("limite", Type.PrimitiveTypes.INTEIRO)))),

                    Map.entry("sen", new Declaration.Function("sen", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),
                    Map.entry("tan", new Declaration.Function("tan", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL))))
            ),
            Map.of("mudacor", new Declaration.Procedure("mudacor", Map.of("cor", new Declaration.Variable("cor", CARACTERE), "localizacao", new Declaration.Variable("localizacao", CARACTERE)))), Map.of(), null);

    public sealed interface Type {
        enum PrimitiveTypes implements Type {
            CARACTERE, INTEIRO, REAL, LOGICO, UNDECLARED, UNDEFINED
        }

        record Array(Type type, int size) implements Type {
        }
    }

    public sealed interface Declaration {
        String name();

        Location location();

        record Variable(String name, Type type, Location location) implements Declaration {
            public Variable {
                name = name.toLowerCase();
            }

            Variable(String valor, Type primitiveTypes) {
                this(valor, primitiveTypes, null);
            }
        }

        record Constant(String name, Type type, Location location) implements Declaration {
            public Constant {
                name = name.toLowerCase();
            }
        }

        record Function(String name, Type returnType, SequencedMap<String, Variable> parameters,
                        Location location) implements Declaration {
            public Function {
                name = name.toLowerCase();
            }

            Function(String name, Type returnType, Map<String, Variable> parameters) {
                this(name, returnType, new LinkedHashMap<>(parameters), null);
            }
        }

        record Procedure(String name, SequencedMap<String, Variable> parameters,
                         Location location) implements Declaration {
            public Procedure {
                name = name.toLowerCase();
            }

            Procedure(String name, Map<String, Variable> parameters) {
                this(name, new LinkedHashMap<>(parameters), null);
            }
        }

        record UserDefinedType(String name, Map<String, Declaration.Variable> variables,
                               Location location) implements Type, Declaration {
            public UserDefinedType {
                name = name.toLowerCase();
            }
        }
    }

    public record Scope(String name, Map<String, Declaration.Variable> variables,
                        Map<String, Declaration.Constant> constants, Map<String, Declaration.Function> functions,
                        Map<String, Declaration.Procedure> procedures,
                        Map<String, Declaration.UserDefinedType> userDefinedTypes, Scope parent) {

        static Scope newScope(String scopeName, Scope parent) {
            return new Scope(scopeName, new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), new HashMap<>(), parent);
        }

        public Optional<Declaration.UserDefinedType> type(String name) {
            return Optional.ofNullable(userDefinedTypes.get(name.toLowerCase())).or(() -> parent == null ? Optional.empty() : parent.type(name));
        }

        public Optional<Declaration.Function> function(String name) {
            return Optional.ofNullable(functions.get(name.toLowerCase())).or(() -> parent == null ? Optional.empty() : parent.function(name));
        }

        public Optional<Declaration.Procedure> procedure(String name) {
            return Optional.ofNullable(procedures.get(name.toLowerCase())).or(() -> parent == null ? Optional.empty() : parent.procedure(name));
        }

        public Optional<Declaration.Variable> variable(String name) {
            return Optional.ofNullable(variables.get(name.toLowerCase())).or(() -> parent == null ? Optional.empty() : parent.variable(name));
        }

        public Optional<Declaration.Constant> constant(String name) {
            return Optional.ofNullable(constants.get(name.toLowerCase())).or(() -> parent == null ? Optional.empty() : parent.constant(name));
        }

        public Optional<? extends Declaration> declaration(String name) {
            return Stream.<Function<String, Optional<? extends Declaration>>>of(
                            this::function,
                            this::procedure,
                            this::variable,
                            this::constant,
                            this::type
                    ).map(optionalSupplier -> optionalSupplier.apply(name.toLowerCase()))
                    .flatMap(Optional::stream)
                    .findFirst();
        }
    }

    public static TypeCheckerResult semanticAnalysis(Node node) {
        List<Error> errors = new ArrayList<>();

        Scope scope = Scope.newScope("GLOBAL", DEFAULT_GLOBAL_SCOPE);

        semanticAnalysis(node, scope, errors);

        return new TypeCheckerResult(Optional.of(node), errors, scope);
    }

    private static void semanticAnalysis(Node node, Scope scope, List<Error> errors) {
        switch (node) {
            case Node.AlgoritimoNode algoritimoNode -> handleAlgoritmoNode(scope, errors, algoritimoNode);
            case Node.DosNode _ -> {
            }
            case Node.FunctionDeclarationNode functionDeclarationNode ->
                    handleFunctionDeclarationNode(scope, errors, functionDeclarationNode);
            case Node.ProcedureDeclarationNode procedureDeclarationNode ->
                    handleProcedureDeclarationNode(scope, errors, procedureDeclarationNode);
            case Node.RegistroDeclarationNode registroDeclarationNode ->
                    handleRegistroDeclarationNode(scope, errors, registroDeclarationNode);
            case Node.VariableDeclarationNode variableDeclarationNode ->
                    handleVariableDeclarationNode(scope, errors, variableDeclarationNode);
            case Node.ConstantNode constantNode -> handleConstantNode(scope, errors, constantNode);
            case Node.CompundNode(var nodes, _) -> handleCompoundNodeSemantic(scope, errors, nodes);
            case Node.CommandNode commandNode -> typeCheckCommand(commandNode, scope, errors);
            default -> errors.add(new Error("Unsupported node: " + node.getClass(), node.location().orElse(Location.EMPTY)));
        }
    }

    private static void handleCompoundNodeSemantic(Scope scope, List<Error> errors, List<? extends Node> nodes) {
        nodes.forEach(n -> semanticAnalysis(n, scope, errors));
    }

    private static void handleConstantNode(Scope scope, List<Error> errors, Node.ConstantNode constantNode) {
        if (scope.constant(constantNode.name().id()).isPresent()) {
            errors.add(new Error("Constant " + constantNode.name().id() + " already declared", constantNode.location().orElse(Location.EMPTY)));
        } else
            scope.constants().put(constantNode.name().id().toLowerCase(), new Declaration.Constant(constantNode.name().id(), getType(constantNode, scope, errors), constantNode.location().orElse(Location.EMPTY)));
    }

    private static void handleVariableDeclarationNode(Scope scope, List<Error> errors, Node.VariableDeclarationNode variableDeclarationNode) {
        scope.variables().put(variableDeclarationNode.name().id().toLowerCase(), new Declaration.Variable(variableDeclarationNode.name().id(), getType(variableDeclarationNode.type(), scope, errors), variableDeclarationNode.location().orElse(Location.EMPTY)));
    }

    private static void handleRegistroDeclarationNode(Scope scope, List<Error> errors, Node.RegistroDeclarationNode registroDeclarationNode) {
        if (scope.variable(registroDeclarationNode.name().id()).isPresent()) {
            errors.add(new Error("Registro " + registroDeclarationNode.name().id() + " already declared", registroDeclarationNode.location().orElse(Location.EMPTY)));
        } else {
            Scope registroScope = Scope.newScope(registroDeclarationNode.name().id(), scope);
            registroDeclarationNode.variableDeclarationContexts().nodes().forEach(variableDeclarationContext -> semanticAnalysis(variableDeclarationContext, registroScope, errors));
            scope.userDefinedTypes.put(registroDeclarationNode.name().id().toLowerCase(), new Declaration.UserDefinedType(registroDeclarationNode.name().id(), registroScope.variables(), registroDeclarationNode.location().orElse(Location.EMPTY)));
        }
    }

    private static void handleProcedureDeclarationNode(Scope scope, List<Error> errors, Node.ProcedureDeclarationNode procedureDeclarationNode) {
        if (scope.procedure(procedureDeclarationNode.name().id()).isPresent()) {
            errors.add(new Error("Procedure " + procedureDeclarationNode.name().id() + " already declared", procedureDeclarationNode.location().orElse(Location.EMPTY)));
        } else {
            SequencedMap<String, Declaration.Variable> parameters = procedureDeclarationNode.parameters().nodes().stream()
                    .filter(x -> x instanceof Node.VariableDeclarationNode)
                    .map(Node.VariableDeclarationNode.class::cast)
                    .map(p -> new Declaration.Variable(p.name().id(), getType(p.type(), scope, errors), p.location().orElse(Location.EMPTY)))
                    .collect(Collectors.toMap(Declaration.Variable::name, x -> x, (a, _) -> a, LinkedHashMap::new));

            scope.procedures.put(procedureDeclarationNode.name().id().toLowerCase(), new Declaration.Procedure(procedureDeclarationNode.name().id(), parameters, procedureDeclarationNode.location().orElse(Location.EMPTY)));

            Scope newScope = Scope.newScope(procedureDeclarationNode.name().id(), scope);
            procedureDeclarationNode.parameters().nodes().forEach(parameter -> semanticAnalysis(parameter, newScope, errors));
            procedureDeclarationNode.declarations().nodes().forEach(declaration -> semanticAnalysis(declaration, newScope, errors));
            procedureDeclarationNode.commands().nodes().forEach(command -> semanticAnalysis(command, newScope, errors));
        }
    }

    private static void handleFunctionDeclarationNode(Scope scope, List<Error> errors, Node.FunctionDeclarationNode functionDeclarationNode) {
        if (scope.function(functionDeclarationNode.name().id()).isPresent()) {
            errors.add(new Error("Function " + functionDeclarationNode.name().id() + " already declared", functionDeclarationNode.location().orElse(Location.EMPTY)));
        } else {
            Type returnType = getType(functionDeclarationNode.returnType(), scope, errors);
            SequencedMap<String, Declaration.Variable> parameters = functionDeclarationNode.parameters().nodes().stream()
                    .filter(x -> x instanceof Node.VariableDeclarationNode)
                    .map(Node.VariableDeclarationNode.class::cast)
                    .map(p -> new Declaration.Variable(p.name().id(), getType(p.type(), scope, errors), p.location().orElse(Location.EMPTY)))
                    .collect(Collectors.toMap(Declaration.Variable::name, x -> x, (a, _) -> a, LinkedHashMap::new));

            scope.functions.put(functionDeclarationNode.name().id().toLowerCase(), new Declaration.Function(functionDeclarationNode.name().id(), returnType, parameters, functionDeclarationNode.location().orElse(Location.EMPTY)));

            Scope newScope = Scope.newScope(functionDeclarationNode.name().id(), scope);
            functionDeclarationNode.parameters().nodes().forEach(parameter -> semanticAnalysis(parameter, newScope, errors));
            functionDeclarationNode.declarations().nodes().forEach(declaration -> semanticAnalysis(declaration, newScope, errors));
            functionDeclarationNode.commands().nodes().forEach(command -> semanticAnalysis(command, newScope, errors));
        }
    }

    private static void handleAlgoritmoNode(Scope scope, List<Error> errors, Node.AlgoritimoNode algoritimoNode) {
        semanticAnalysis(algoritimoNode.declarations(), scope, errors);
        semanticAnalysis(algoritimoNode.commands(), scope, errors);
    }

    private static void typeCheckCommand(Node command, Scope scope, List<Error> errors) {
        switch (command) {
            case Node.InterrompaCommandNode interrompaCommandNode ->
                    handleInterrompaCommand(scope, errors, interrompaCommandNode);
            case Node.ReturnNode(var node, var location) -> handleReturnNode(scope, errors, node, location.orElse(Location.EMPTY));

            case Node.DosNode dosNode -> handleDosNode(scope, errors, dosNode);

            case Node.AssignmentNode assignmentNode -> handleAssignmentNode(scope, errors, assignmentNode);

            case Node.ForCommandNode forCommandNode -> handleForCommand(scope, errors, forCommandNode);

            case Node.WhileCommandNode whileCommandNode -> handleWhileCommand(scope, errors, whileCommandNode);

            case Node.ArrayAccessNode arrayAccessNode -> handleArrayAccessNode(scope, errors, arrayAccessNode);

            case Node.IdNode idNode -> handleIdNode(scope, errors, idNode);

            case Node.DivNode(Node left, Node right, _,var location) ->
                    handleDivNode(scope, errors, left, right, location.orElse(Location.EMPTY));

            case Node.ModNode(Node left, Node right, var location) ->
                    handleModNode(scope, errors, left, right, location.orElse(Location.EMPTY));

            case Node.AddNode(Node left, Node right, var location) ->
                    handleAddNode(scope, errors, left, right, location.orElse(Location.EMPTY));

            case Node.SubNode(Node left, Node right, var location) ->
                    handleSubNode(scope, errors, left, right, location.orElse(Location.EMPTY));

            case Node.MulNode(Node left, Node right, var location) ->
                    handleMulNode(scope, errors, left, right, location.orElse(Location.EMPTY));

            case Node.PowNode(Node left, Node right, var location) ->
                    handlePowNode(scope, errors, left, right, location.orElse(Location.EMPTY));

            case Node.AndNode(Node left, Node right, var location) ->
                    handleAndNode(scope, errors, left, right, location.orElse(Location.EMPTY));

            case Node.OrNode(Node left, Node right, var location) ->
                    handleOrNode(scope, errors, left, right, location.orElse(Location.EMPTY));

            case Node.NotNode(Node expr, var location) -> handleNotNode(scope, errors, expr, location.orElse(Location.EMPTY));

            case Node.EqNode(Node left, Node right, var location) ->
                    handleEqNode(scope, errors, left, right, location.orElse(Location.EMPTY));

            case Node.NeNode(Node left, Node right, var location) ->
                    handleNeNode(scope, errors, left, right, location.orElse(Location.EMPTY));

            case Node.LtNode(Node left, Node right, var location) ->
                    handleLtNode(scope, errors, left, right, location.orElse(Location.EMPTY));

            case Node.LeNode(Node left, Node right, var location) ->
                    handleLeNode(scope, errors, left, right, location.orElse(Location.EMPTY));

            case Node.GtNode(Node left, Node right, var location) ->
                    handleGtNode(scope, errors, left, right, location.orElse(Location.EMPTY));

            case Node.GeNode(Node left, Node right, var location) ->
                    handleGeNode(scope, errors, left, right, location.orElse(Location.EMPTY));

            case Node.NegNode(Node expr, var location) -> handleNegNode(scope, errors, expr, location.orElse(Location.EMPTY));

            case Node.ConditionalCommandNode ifNode -> handleConditionalCommand(scope, errors, ifNode);

            case Node.ProcedureCallNode procedureCallNode -> handleProcedureCallNode(scope, errors, procedureCallNode);

            case Node.FunctionCallNode functionCallNode -> handleFunctionCallNode(scope, errors, functionCallNode);

            case Node.AleatorioNode aleatorioRangeNode -> handleAleatorioCommandNode(errors, aleatorioRangeNode);

            case Node.TimerCommandNode timerCommandNode -> handleTimerCommand(errors, timerCommandNode);

            case Node.ChooseCommandNode commandNode -> handleChooseCommandNode(scope, errors, commandNode);

            case Node.ReadCommandNode readCommandNode -> handleReadCommand(scope, errors, readCommandNode);

            case Node.WriteItemNode writeItemNode -> handleWriteItemNode(scope, errors, writeItemNode);
            case Node.WriteCommandNode writeCommandNode -> handleWriteCommandNode(scope, errors, writeCommandNode);
            case Node.LimpatelaCommandNode _, Node.EcoCommandNode _, Node.DebugCommandNode _, Node.PausaCommandNode _,
                 Node.CronometroCommandNode _ -> {
            }
            default -> errors.add(new Error("Unsupported command node: " + command.getClass(), command.location().orElse(Location.EMPTY)));
        }
    }

    private static void handleWriteCommandNode(Scope scope, List<Error> errors, Node.WriteCommandNode writeCommandNode) {
        writeCommandNode.writeList().nodes().forEach(x -> semanticAnalysis(x, scope, errors));
    }

    private static void handleWriteItemNode(Scope scope, List<Error> errors, Node.WriteItemNode writeItemNode) {
        Type type = getType(writeItemNode.expr(), scope, errors);
        if (type == PrimitiveTypes.UNDECLARED) {
            errors.add(new Error("Write command on undeclared type", writeItemNode.expr().location().orElse(Location.EMPTY)));
        }
        if (writeItemNode.precision() != Node.EmptyNode.INSTANCE && type != PrimitiveTypes.REAL) {
            errors.add(new Error("Write command with precision on non-real type: " + type, writeItemNode.expr().location().orElse(Location.EMPTY)));
        }
    }

    private static void handleReadCommand(Scope scope, List<Error> errors, Node.ReadCommandNode readCommandNode) {
        for (Node id : readCommandNode.exprList().nodes()) {
            Type idType = getType(id, scope, errors);
            if (idType == PrimitiveTypes.UNDECLARED) {
                errors.add(new Error("Read command on undeclared type", id.location().orElse(Location.EMPTY)));
            } else if (idType instanceof Array) {
                errors.add(new Error("Read command on array type: " + idType, id.location().orElse(Location.EMPTY)));
            }
        }
    }

    private static void handleChooseCommandNode(Scope scope, List<Error> errors, Node.ChooseCommandNode commandNode) {
        Type type = getType(commandNode.expr(), scope, errors);
        if (!areNumbers(type, REAL) && type != CARACTERE) {
            errors.add(new Error("Choose command with non-integer type: " + type, commandNode.expr().location().orElse(Location.EMPTY)));
        }
        commandNode.cases().nodes().forEach(cases -> {
            cases.value().nodes().forEach(caseNode -> {
                if (!areTypesCompatible(type, getType(caseNode, scope, errors))) {
                    errors.add(new Error("Choose case with different types: " + type + " and " + getType(caseNode, scope, errors), caseNode.location().orElse(Location.EMPTY)));
                } else {
                    cases.commands().nodes().forEach(caseCommand -> typeCheckCommand(caseCommand, scope, errors));
                }
            });
        });
    }

    private static void handleTimerCommand(List<Error> errors, Node.TimerCommandNode timerCommandNode) {
        if (timerCommandNode.value() < 0) {
            errors.add(new Error("Timer command with negative argument: " + timerCommandNode.value(), timerCommandNode.location().orElse(Location.EMPTY)));
        }
    }

    private static void handleAleatorioCommandNode(List<Error> errors, Node.AleatorioNode aleatorioRangeNode) {
        switch (aleatorioRangeNode) {
            case Node.AleatorioOffNode aleatorioOffNode -> {

            }
            case Node.AleatorioOnNode aleatorioOnNode -> {
            }
            case Node.AleatorioRangeNode rangeNode -> {
                Type startType = getType(rangeNode.start(), null, errors);
                if (!areNumbers(startType, INTEIRO)) {
                    errors.add(new Error("Aleatorio command with non-integer type: " + startType, rangeNode.start().location().orElse(Location.EMPTY)));
                }
                Type endType = getType(rangeNode.end(), null, errors);
                if (!areNumbers(startType, INTEIRO)) {
                    errors.add(new Error("Aleatorio command with non-integer type: " + endType, rangeNode.end().location().orElse(Location.EMPTY)));
                }
                Type type = getType(rangeNode.decimalPlaces(), null, errors);
                if (!areNumbers(type, INTEIRO)) {
                    errors.add(new Error("Aleatorio command with non-integer type: " + type, rangeNode.decimalPlaces().location().orElse(Location.EMPTY)));
                }
            }
        }
    }

    private static void handleFunctionCallNode(Scope scope, List<Error> errors, Node.FunctionCallNode functionCallNode) {
        Optional<Declaration.Function> function = scope.function(functionCallNode.name().id());
        if (function.isEmpty()) {
            errors.add(new Error("Function " + functionCallNode.name().id() + " not declared", functionCallNode.location().orElse(Location.EMPTY)));
            return;
        }
        if (functionCallNode.args().nodes().size() != function.get().parameters().size()) {
            errors.add(new Error("Function " + functionCallNode.name().id() + "called with wrong number of arguments. Expected: " + function.get().parameters().size() + " but got: " + functionCallNode.args().nodes().size(), functionCallNode.location().orElse(Location.EMPTY)));
        }
    }

    private static void handleProcedureCallNode(Scope scope, List<Error> errors, Node.ProcedureCallNode procedureCallNode) {
        Optional<Declaration.Procedure> procedure = scope.procedure(procedureCallNode.name().id());
        if (procedure.isEmpty()) {
            errors.add(new Error("Procedure " + procedureCallNode.name().id() + " not declared", procedureCallNode.location().orElse(Location.EMPTY)));
            return;
        }
        if (procedureCallNode.args().nodes().size() != procedure.get().parameters().size()) {
            errors.add(new Error("Procedure " + procedureCallNode.name().id() + "called with wrong number of arguments. Expected: " + procedure.get().parameters().size() + " but got: " + procedureCallNode.args().nodes().size(), procedureCallNode.location().orElse(Location.EMPTY)));
        }
    }

    private static void handleConditionalCommand(Scope scope, List<Error> errors, Node.ConditionalCommandNode ifNode) {
        Type conditionType = getType(ifNode.expr(), scope, errors);
        if (conditionType != PrimitiveTypes.LOGICO) {
            errors.add(new Error("If command with non-boolean condition:" + conditionType, ifNode.expr().location().orElse(Location.EMPTY)));
        }
        ifNode.commands().nodes().forEach(c -> typeCheckCommand(c, scope, errors));
        ifNode.elseCommands().nodes().forEach(c -> typeCheckCommand(c, scope, errors));
    }

    private static void handleNegNode(Scope scope, List<Error> errors, Node expr, Location location) {
        Type exprType = getType(expr, scope, errors);
        if (!areNumbers(exprType, PrimitiveTypes.REAL)) {
            errors.add(new Error("Neg command with non number type: " + exprType, location));
        }
    }

    private static void handleGeNode(Scope scope, List<Error> errors, Node left, Node right, Location location) {
        Type leftType = getType(left, scope, errors);
        Type rightType = getType(right, scope, errors);
        if (!areTypesCompatible(leftType, rightType)) {
            errors.add(new Error("Ge command with different types: " + leftType + " and " + rightType, location));
        }
    }

    private static void handleGtNode(Scope scope, List<Error> errors, Node left, Node right, Location location) {
        Type leftType = getType(left, scope, errors);
        Type rightType = getType(right, scope, errors);
        if (!areTypesCompatible(leftType, rightType)) {
            errors.add(new Error("Gt command with different types: " + leftType + " and " + rightType, location));
        }
    }

    private static void handleLeNode(Scope scope, List<Error> errors, Node left, Node right, Location location) {
        Type leftType = getType(left, scope, errors);
        Type rightType = getType(right, scope, errors);
        if (!areTypesCompatible(leftType, rightType)) {
            errors.add(new Error("Le command with different types: " + leftType + " and " + rightType, location));
        }
    }

    private static void handleLtNode(Scope scope, List<Error> errors, Node left, Node right, Location location) {
        Type leftType = getType(left, scope, errors);
        Type rightType = getType(right, scope, errors);
        if (!areTypesCompatible(leftType, rightType)) {
            errors.add(new Error("Lt command with different types: " + leftType + " and " + rightType, location));
        }
    }

    private static void handleNeNode(Scope scope, List<Error> errors, Node left, Node right, Location location) {
        Type leftType = getType(left, scope, errors);
        Type rightType = getType(right, scope, errors);
        if (!areTypesCompatible(leftType, rightType)) {
            errors.add(new Error("Ne command with different types: " + leftType + " and " + rightType, location));
        }
    }

    private static void handleEqNode(Scope scope, List<Error> errors, Node left, Node right, Location location) {
        Type leftType = getType(left, scope, errors);
        Type rightType = getType(right, scope, errors);
        if (!areTypesCompatible(leftType, rightType)) {
            errors.add(new Error("Eq command with different types: " + leftType + " and " + rightType, location));
        }
    }

    private static void handleNotNode(Scope scope, List<Error> errors, Node expr, Location location) {
        Type exprType = getType(expr, scope, errors);
        if (exprType != PrimitiveTypes.LOGICO) {
            errors.add(new Error("Not command with non-boolean type: " + exprType, location));
        }
    }

    private static void handleOrNode(Scope scope, List<Error> errors, Node left, Node right, Location location) {
        Type leftType = getType(left, scope, errors);
        Type rightType = getType(right, scope, errors);
        if (!areThey(LOGICO, leftType, rightType)) {
            errors.add(new Error("Or command with non-boolean types: " + leftType + " and " + rightType, location));
        }
    }

    private static void handleAndNode(Scope scope, List<Error> errors, Node left, Node right, Location location) {
        Type leftType = getType(left, scope, errors);
        Type rightType = getType(right, scope, errors);
        if (!areThey(LOGICO, leftType, rightType)) {
            errors.add(new Error("And command with non-boolean types: " + leftType + " and " + rightType, location));
        }
    }

    private static void handlePowNode(Scope scope, List<Error> errors, Node left, Node right, Location location) {
        Type leftType = getType(left, scope, errors);
        Type rightType = getType(right, scope, errors);
        if (!areNumbers(leftType, rightType)) {
            errors.add(new Error("Pow command with non-number types: " + leftType + " and " + rightType, location));
        }
    }

    private static void handleMulNode(Scope scope, List<Error> errors, Node left, Node right, Location location) {
        Type leftType = getType(left, scope, errors);
        Type rightType = getType(right, scope, errors);
        if (!areNumbers(leftType, rightType)) {
            errors.add(new Error("Mul command with non-number types: " + leftType + " and " + rightType, location));
        }
    }

    private static void handleSubNode(Scope scope, List<Error> errors, Node left, Node right, Location location) {
        Type leftType = getType(left, scope, errors);
        Type rightType = getType(right, scope, errors);
        if (!areNumbers(leftType, rightType)) {
            errors.add(new Error("Sub command with non-number types: " + leftType + " and " + rightType, location));
        }
    }

    private static void handleAddNode(Scope scope, List<Error> errors, Node left, Node right, Location location) {
        Type leftType = getType(left, scope, errors);
        Type rightType = getType(right, scope, errors);
        if (leftType != rightType) {
            errors.add(new Error("Add command with different types: " + leftType + " and " + rightType, location));
        } else if (leftType != PrimitiveTypes.INTEIRO && leftType != PrimitiveTypes.REAL && leftType != CARACTERE) {
            errors.add(new Error("Add command with non-integer, real or character types: " + leftType, location));
        }
    }

    private static void handleModNode(Scope scope, List<Error> errors, Node left, Node right, Location location) {
        Type leftType = getType(left, scope, errors);
        Type rightType = getType(right, scope, errors);
        if (leftType != PrimitiveTypes.INTEIRO || rightType != PrimitiveTypes.INTEIRO) {
            errors.add(new Error("Mod command with non-integer types: " + leftType + " and " + rightType, location));
        }
    }

    private static void handleDivNode(Scope scope, List<Error> errors, Node left, Node right, Location location) {
        Type leftType = getType(left, scope, errors);
        Type rightType = getType(right, scope, errors);
        if (leftType != PrimitiveTypes.INTEIRO || rightType != PrimitiveTypes.INTEIRO) {
            errors.add(new Error("Div command with non-integer types: " + leftType + " and " + rightType, location));
        }
    }

    private static void handleIdNode(Scope scope, List<Error> errors, Node.IdNode idNode) {
        if (scope.declaration(idNode.id()).isEmpty()) {
            errors.add(new Error(idNode.id() + " not declared", idNode.location().orElse(Location.EMPTY)));
        }
    }

    private static void handleArrayAccessNode(Scope scope, List<Error> errors, Node.ArrayAccessNode arrayAccessNode) {
        Type idType = getType(arrayAccessNode.node(), scope, errors);
        if (!(idType instanceof Array)) {
            errors.add(new Error("Array access on a non-array type:" + idType, arrayAccessNode.node().location().orElse(Location.EMPTY)));
        }
        for (Node index : arrayAccessNode.indexes().nodes()) {
            Type indexType = getType(index, scope, errors);
            if (indexType != PrimitiveTypes.INTEIRO) {
                errors.add(new Error("Array access with non-integer index: " + indexType, index.location().orElse(Location.EMPTY)));
            }
        }
    }

    private static void handleWhileCommand(Scope scope, List<Error> errors, Node.WhileCommandNode whileCommandNode) {
        Type testType = getType(whileCommandNode.test(), scope, errors);
        if (testType != PrimitiveTypes.LOGICO) {
            errors.add(new Error("While command with non-boolean type: " + testType, whileCommandNode.test().location().orElse(Location.EMPTY)));
        }
        whileCommandNode.commands().nodes().forEach(x -> typeCheckCommand(x, scope, errors));
    }

    private static void handleForCommand(Scope scope, List<Error> errors, Node.ForCommandNode forCommandNode) {
        Node.IdNode identifier = forCommandNode.identifier();
        if (scope.variable(identifier.id()).isEmpty()) {
            errors.add(new Error("For command with undeclared variable: " + identifier.id(), identifier.location().orElse(Location.EMPTY)));
        }

        Type startType = getType(forCommandNode.startValue(), scope, errors);
        Type endType = getType(forCommandNode.endValue(), scope, errors);
        Type stepType = getType(forCommandNode.step(), scope, errors);
        if (startType != PrimitiveTypes.INTEIRO) {
            errors.add(new Error("For start command with non-integer types: " + startType, forCommandNode.startValue().location().orElse(Location.EMPTY)));
        }
        if (endType != PrimitiveTypes.INTEIRO && endType != PrimitiveTypes.UNDEFINED) {
            errors.add(new Error("For end command with non-integer types: " + endType, forCommandNode.endValue().location().orElse(Location.EMPTY)));
        }
        if (stepType != PrimitiveTypes.INTEIRO && stepType != PrimitiveTypes.UNDECLARED) {
            errors.add(new Error("For step command with non-integer types: " + stepType, forCommandNode.startValue().location().orElse(Location.EMPTY)));
        }
        forCommandNode.commands().nodes().forEach(x -> typeCheckCommand(x, scope, errors));
    }

    private static void handleAssignmentNode(Scope scope, List<Error> errors, Node.AssignmentNode assignmentNode) {
        Type idType = getType(assignmentNode.idOrArray(), scope, errors);
        Type exprType = getType(assignmentNode.expr(), scope, errors);

        if (!areTypesCompatible(idType, exprType)) {
            errors.add(new Error("Assignment of different types: " + idType + " and " + exprType, assignmentNode.expr().location().orElse(Location.EMPTY)));
        }
    }

    private static void handleDosNode(Scope scope, List<Error> errors, Node.DosNode dosNode) {
        if (scope.parent() != null) {
            errors.add(new Error("Dos command inside a function or procedure", dosNode.location().orElse(Location.EMPTY)));
        }
    }

    private static void handleReturnNode(Scope scope, List<Error> errors, Node node, Location location) {
        if (scope.parent() == null) {
            errors.add(new Error("Return command outside of a function", location));
        }
        Type type = getType(node, scope, errors);
        Optional<Declaration.Function> function = scope.function(scope.name);
        if (function.isEmpty()) {
            errors.add(new Error("Should have a function", location));

        } else {
            if (!areTypesCompatible(type, function.get().returnType())) {
                errors.add(new Error("Return type different from function type: " + function.get().returnType() + " and " + type, location));
            }
        }
    }

    private static void handleInterrompaCommand(Scope scope, List<Error> errors, Node.InterrompaCommandNode interrompaCommandNode) {
        if (scope.parent() == null) {
            errors.add(new Error("Interrompa command outside of a function or procedure", interrompaCommandNode.location().orElse(Location.EMPTY)));
        }
    }

    private static Type getType(Node node, Scope scope, List<Error> errors) {
        return switch (node) {
            case Node.EmptyNode _ -> Type.PrimitiveTypes.UNDEFINED;
            case Node.TypeNode(Node.StringLiteralNode(String stringValue, _), var location) -> switch (stringValue.toLowerCase()) {
                case "inteiro" -> Type.PrimitiveTypes.INTEIRO;
                case "real", "numerico" -> Type.PrimitiveTypes.REAL;
                case "logico" -> Type.PrimitiveTypes.LOGICO;
                case "caractere", "caracter", "literal" -> CARACTERE;
                default -> {
                    Optional<Declaration.UserDefinedType> userDefinedType = scope.type(stringValue);
                    if (userDefinedType.isPresent()) {
                        yield userDefinedType.get();
                    } else {
                        errors.add(new Error("Type " + stringValue + " not declared", location.orElse(Location.EMPTY)));
                        yield Type.PrimitiveTypes.UNDECLARED;
                    }
                }
            };
            case Node.ArrayAccessNode arrayAccessNode -> {
                Type idType = getType(arrayAccessNode.node(), scope, errors);
                if (idType instanceof Type.Array arrayType) {
                    yield arrayType.type();
                } else {
                    yield idType;
                }
            }
            case Node.ConstantNode constantNode -> getType(constantNode.value(), scope, errors);
            case Node.MemberAccessNode memberAccessNode -> {
                Type idType = getType(memberAccessNode.node(), scope, errors);
                if (idType instanceof Declaration.UserDefinedType userDefinedType) {
                    Node member = memberAccessNode.member();
                    if (!(member instanceof Node.IdNode idNode)) {
                        errors.add(new Error("Member access with non-id node: " + member, member.location().orElse(Location.EMPTY)));
                        yield Type.PrimitiveTypes.UNDECLARED;
                    }
                    Declaration.Variable variable = userDefinedType.variables().get(idNode.id().toLowerCase());
                    if (variable == null) {
                        errors.add(new Error("Member " + member + " not declared in type " + userDefinedType.name(), member.location().orElse(Location.EMPTY)));
                        yield Type.PrimitiveTypes.UNDECLARED;
                    }
                    yield variable.type();
                } else {
                    errors.add(new Error("Member access on a non-user defined type: " + idType, memberAccessNode.location().orElse(Location.EMPTY)));
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
            }
            case Node.BooleanLiteralNode _ -> Type.PrimitiveTypes.LOGICO;
            case Node.IntLiteralNode _ -> Type.PrimitiveTypes.INTEIRO;
            case Node.RealLiteralNode _ -> Type.PrimitiveTypes.REAL;
            case Node.StringLiteralNode _ -> CARACTERE;

            case Node.NegNode negNode -> getType(negNode.expr(), scope, errors);

            case Node.ArrayTypeNode arrayTypeNode -> {
                Type type = getType(arrayTypeNode.type(), scope, errors);
                if (type == null) {
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
                yield new Type.Array(type, 1);
            }

            case Node.IdNode idNode -> {
                Optional<? extends Declaration> declaration = scope.declaration(idNode.id());
                if (declaration.isEmpty()) {
                    errors.add(new Error(idNode.id() + " not declared", idNode.location().orElse(Location.EMPTY)));
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
                yield getType(declaration.get());
            }

            case Node.BinaryNode binaryNode -> getType(binaryNode, scope, errors);

            case Node.NotNode notNode -> {
                Type exprType = getType(notNode.expr(), scope, errors);
                if (exprType == Type.PrimitiveTypes.LOGICO) {
                    yield Type.PrimitiveTypes.LOGICO;
                } else {
                    errors.add(new Error("Not operation with non-boolean type: " + exprType, notNode.location().orElse(Location.EMPTY)));
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
            }

            case Node.ProcedureCallNode procedureCallNode -> {
                if (scope.procedure(procedureCallNode.name().id()).isPresent()) {
                    errors.add(new Error("Procedure " + procedureCallNode.name().id() + " does not return a value", procedureCallNode.location().orElse(Location.EMPTY)));
                }
                yield Type.PrimitiveTypes.UNDECLARED;
            }
            case Node.FunctionCallNode functionCallNode -> {
                Optional<Declaration.Function> function = scope.function(functionCallNode.name().id());
                if (function.isEmpty()) {
                    errors.add(new Error("Function " + functionCallNode.name().id() + " not declared", functionCallNode.location().orElse(Location.EMPTY)));
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
                yield function.get().returnType();
            }

            case Node.CompundNode<?> _ -> null;

            case Node.RangeNode(Node start, Node end, var location) -> {
                Type startType = getType(start, scope, errors);

                if (end == null) {
                    yield startType;
                }
                Type endType = getType(end, scope, errors);

                if (areTypesCompatible(startType, endType)) {
                    if (startType == CARACTERE) {
                        yield CARACTERE;
                    } else {
                        yield getGeneralNumberType(startType, endType);
                    }
                } else {
                    errors.add(new Error("Binary operation with non-primitive types: " + startType + " and " + endType, location.orElse(Location.EMPTY)));
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
            }
            default -> {
                errors.add(new Error("Unsupported type node: " + node.getClass(), node.location().orElse(Location.EMPTY)));
                yield Type.PrimitiveTypes.UNDECLARED;
            }

        };
    }

    private static Type getType(Declaration declaration) {
        return switch (declaration) {
            case Declaration.Variable(_, var type, _) -> type;
            case Declaration.Constant(_, var type, _) -> type;
            case Declaration.Function(_, var returnType, _, _) -> returnType;
            case Declaration.Procedure _ -> Type.PrimitiveTypes.UNDECLARED;
            case Declaration.UserDefinedType userDefinedType -> userDefinedType;
        };
    }

    private static Type getType(Node.BinaryNode node, Scope scope, List<Error> errors) {
        return switch (node) {
            case Node.AddNode _ -> {
                Type leftType = getType(node.left(), scope, errors);
                Type rightType = getType(node.right(), scope, errors);
                if (areThey(CARACTERE, leftType, rightType)) {
                    yield CARACTERE;
                } else if (areNumbers(leftType, rightType)) {
                    yield getGeneralNumberType(leftType, rightType);
                } else {
                    errors.add(new Error("Binary operation with non-primitive types: " + leftType + " and " + rightType, node.location().orElse(Location.EMPTY)));
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
            }

            case Node.SubNode _, Node.MulNode _, Node.DivNode _, Node.ModNode _, Node.PowNode _ -> {
                Type leftType = getType(node.left(), scope, errors);
                Type rightType = getType(node.right(), scope, errors);
                if (areNumbers(leftType, rightType)) {
                    yield getGeneralNumberType(leftType, rightType);
                } else {
                    errors.add(new Error("Binary operation with non-primitive types: " + leftType + " and " + rightType, node.location().orElse(Location.EMPTY)));
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
            }
            case Node.AndNode _, Node.OrNode _ -> {
                Type leftType = getType(node.left(), scope, errors);
                Type rightType = getType(node.right(), scope, errors);
                if (areThey(Type.PrimitiveTypes.LOGICO, leftType, rightType)) {
                    yield Type.PrimitiveTypes.LOGICO;
                } else {
                    errors.add(new Error("Binary operation with non-boolean types: " + leftType + " and " + rightType, node.location().orElse(Location.EMPTY)));
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
            }
            case Node.EqNode _, Node.NeNode _, Node.LtNode _, Node.LeNode _, Node.GtNode _, Node.GeNode _ -> {
                Type leftType = getType(node.left(), scope, errors);
                Type rightType = getType(node.right(), scope, errors);
                if (areTypesCompatible(leftType, rightType)) {
                    yield Type.PrimitiveTypes.LOGICO;
                } else {
                    errors.add(new Error("Binary operation with different types: " + leftType + " and " + rightType, node.location().orElse(Location.EMPTY)));
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
            }
        };
    }


    private static boolean areTypesCompatible(Type left, Type right) {
        return left == right || areNumbers(left, right);
    }

    private static boolean areThey(Type resultType, Type left, Type right) {
        if (areTypesCompatible(left, right)) {
            return areTypesCompatible(resultType, left);
        }
        return false;
    }

    private static boolean areNumbers(Type left, Type right) {
        return (left == Type.PrimitiveTypes.INTEIRO || left == Type.PrimitiveTypes.REAL) && (right == Type.PrimitiveTypes.INTEIRO || right == Type.PrimitiveTypes.REAL);
    }


    private static Type getGeneralNumberType(Type left, Type right) {
        if (left == Type.PrimitiveTypes.REAL || right == Type.PrimitiveTypes.REAL) {
            return Type.PrimitiveTypes.REAL;
        }
        return Type.PrimitiveTypes.INTEIRO;
    }


}
