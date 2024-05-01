package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.frontend.node.Location;
import dev.thihup.jvisualg.frontend.node.Node;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.thihup.jvisualg.frontend.TypeChecker.Type.PrimitiveTypes.*;

public class TypeChecker {

    private static final Scope DEFAULT_GLOBAL_SCOPE = new Scope("DEFAULT", Map.of(), Map.of(),
        Map.ofEntries(
            Map.entry("abs", new Declaration.Function("abs", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),
            Map.entry("arccos", new Declaration.Function("arccos", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),
            Map.entry("arcsen", new Declaration.Function("arcsen", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),
            Map.entry("arctan", new Declaration.Function("arctan", Type.PrimitiveTypes.REAL, Map.of("valor", new Declaration.Variable("valor", Type.PrimitiveTypes.REAL)))),
            Map.entry("asc", new Declaration.Function("asc", Type.PrimitiveTypes.INTEIRO, Map.of("s", new Declaration.Variable("s", CARACTERE)))),


            Map.entry("carac", new Declaration.Function("carac", CARACTERE, Map.of("c", new Declaration.Variable("c", Type.PrimitiveTypes.INTEIRO)))),
            Map.entry("caracpnum", new Declaration.Function("caracpnum", Type.PrimitiveTypes.INTEIRO, Map.of("c", new Declaration.Variable("c", CARACTERE)))),
            Map.entry("compr", new Declaration.Function("compr", Type.PrimitiveTypes.INTEIRO, Map.of("c", new Declaration.Variable("c", CARACTERE)))),

            Map.entry("copia", new Declaration.Function("copia", CARACTERE, Map.of("c", new Declaration.Variable("c", CARACTERE),  "p", new Declaration.Variable("p", Type.PrimitiveTypes.INTEIRO), "n", new Declaration.Variable("n", Type.PrimitiveTypes.INTEIRO)))),
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
            Map.entry("randi", new Declaration.Function("randi", Type.PrimitiveTypes.INTEIRO, Map.of("limite", new Declaration.Variable("limite", Type.PrimitiveTypes.INTEIRO))))

        ),
        Map.of("mudacor", new Declaration.Procedure("mudacor", Map.of("letra", new Declaration.Variable("letra", CARACTERE), "fundo", new Declaration.Variable("fundo", CARACTERE)))), Map.of(), null);

    sealed interface Type {
        enum PrimitiveTypes implements Type {
            CARACTERE, INTEIRO, REAL, LOGICO, UNDECLARED, UNDEFINED
        }

        record Array(Type type, int size) implements Type {
        }
    }

    sealed interface Declaration {
        String name();

        record Variable(String name, Type type) implements Declaration {
            public Variable {
                name = name.toLowerCase();
            }
        }

        record Constant(String name, Type type) implements Declaration {
            public Constant { name = name.toLowerCase(); }
        }

        record Function(String name, Type returnType, Map<String, Variable> parameters) implements Declaration {
            public Function { name = name.toLowerCase(); }
        }

        record Procedure(String name, Map<String, Variable> parameters) implements Declaration {
            public Procedure { name = name.toLowerCase(); }
        }

        record UserDefinedType(String name, Map<String, Declaration.Variable> variables) implements Type, Declaration {
            public UserDefinedType { name = name.toLowerCase(); }
        }
    }

    record Scope(String name, Map<String, Declaration.Variable> variables, Map<String, Declaration.Constant> constants, Map<String, Declaration.Function> functions, Map<String, Declaration.Procedure> procedures, Map<String, Declaration.UserDefinedType> userDefinedTypes, Scope parent) {

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

        public <T extends Declaration> Optional<T> declaration(String name) {
            return Stream.<Function<String, Optional<T>>>of(
                (n) -> (Optional<T>) function(n),
                (n) -> (Optional<T>) procedure(n),
                (n) -> (Optional<T>) variable(n),
                (n) -> (Optional<T>) constant(n),
                (n) -> (Optional<T>) type(n)
            ).map(optionalSupplier -> optionalSupplier.apply(name.toLowerCase())).flatMap(Optional::stream).findFirst();
        }
    }

    public static List<String> semanticAnalysis(Node node) {
        List<String> errors = new ArrayList<>();

        Scope scope = Scope.newScope("GLOBAL", DEFAULT_GLOBAL_SCOPE);

        semanticAnalysis(node, scope, errors);
        return errors;
    }

    private static void semanticAnalysis(Node node, Scope scope, List<String> errors) {
        switch (node) {
            case Node.AlgoritimoNode algoritimoNode -> {
                semanticAnalysis(algoritimoNode.declarations(), scope, errors);
                semanticAnalysis(algoritimoNode.commands(), scope, errors);
            }
            case Node.DosNode _ -> {}

            case Node.FunctionDeclarationNode functionDeclarationNode -> {
                if (scope.function(functionDeclarationNode.name()).isPresent()) {
                    errors.add("Function " + functionDeclarationNode.name() + " already declared");
                } else {
                    Type returnType = getType(functionDeclarationNode.returnType(), scope, errors);
                    Map<String, Declaration.Variable> parameters = functionDeclarationNode.parameters().stream()
                            .filter(x -> x instanceof Node.VariableDeclarationNode)
                            .map(Node.VariableDeclarationNode.class::cast)
                            .map(p -> new Declaration.Variable(p.name(), getType(p.type(), scope, errors)))
                            .collect(Collectors.toMap(Declaration.Variable::name, x -> x));

                    scope.functions.put(functionDeclarationNode.name().toLowerCase(), new Declaration.Function(functionDeclarationNode.name(), returnType, parameters));

                    Scope newScope = Scope.newScope(functionDeclarationNode.name(), scope);
                    functionDeclarationNode.parameters().forEach(parameter -> semanticAnalysis(parameter, newScope, errors));
                    functionDeclarationNode.declarations().forEach(declaration -> semanticAnalysis(declaration, newScope, errors));
                    functionDeclarationNode.commands().forEach(command -> semanticAnalysis(command, newScope, errors));
                }
            }
            case Node.ProcedureDeclarationNode procedureDeclarationNode -> {
                if (scope.procedure(procedureDeclarationNode.name()).isPresent()) {
                    errors.add("Procedure " + procedureDeclarationNode.name() + " already declared");
                } else {
                    Map<String, Declaration.Variable> parameters = procedureDeclarationNode.parameters().stream()
                            .filter(x -> x instanceof Node.VariableDeclarationNode)
                            .map(Node.VariableDeclarationNode.class::cast)
                            .map(p -> new Declaration.Variable(p.name(), getType(p.type(), scope, errors)))
                            .collect(Collectors.toMap(Declaration.Variable::name, x -> x));

                    scope.procedures.put(procedureDeclarationNode.name().toLowerCase(), new Declaration.Procedure(procedureDeclarationNode.name(), parameters));

                    Scope newScope = Scope.newScope(procedureDeclarationNode.name(), scope);
                    procedureDeclarationNode.parameters().forEach(parameter -> semanticAnalysis(parameter, newScope, errors));
                    procedureDeclarationNode.declarations().forEach(declaration -> semanticAnalysis(declaration, newScope, errors));
                    procedureDeclarationNode.commands().forEach(command -> semanticAnalysis(command, newScope, errors));
                }
            }
            case Node.RegistroDeclarationNode registroDeclarationNode -> {
                if (scope.variable(registroDeclarationNode.name()).isPresent()) {
                    errors.add("Registro " + registroDeclarationNode.name() + " already declared");
                } else {
                    Scope registroScope = Scope.newScope(registroDeclarationNode.name(), scope);
                    registroDeclarationNode.variableDeclarationContexts().forEach(variableDeclarationContext -> semanticAnalysis(variableDeclarationContext, registroScope, errors));
                    scope.userDefinedTypes.put(registroDeclarationNode.name().toLowerCase(), new Declaration.UserDefinedType(registroDeclarationNode.name(), registroScope.variables()));
                }
            }
            case Node.VariableDeclarationNode variableDeclarationNode -> {
                scope.variables().put(variableDeclarationNode.name().toLowerCase(), new Declaration.Variable(variableDeclarationNode.name(), getType(variableDeclarationNode.type(), scope, errors)));
            }
            case Node.ConstantsDeclarationNode constantsDeclarationNode -> {
                constantsDeclarationNode.constants().forEach(constant -> semanticAnalysis(constant, scope, errors));
            }
            case Node.ConstantNode constantNode -> {
                if (scope.constant(constantNode.name()).isPresent()) {
                    errors.add("Constant " + constantNode.name() + " already declared");
                } else scope.constants().put(constantNode.name().toLowerCase(), new Declaration.Constant(constantNode.name(), getType(constantNode, scope, errors)));
            }
            case Node.CommandsNode commandsNode -> {
                commandsNode.commands().forEach(command -> semanticAnalysis(command, scope, errors));
            }
            case Node.CommandNode commandNode -> typeCheckCommand(commandNode.command(), scope, errors);

            case Node.CompundNode(var nodes, _) ->  nodes.forEach(n -> semanticAnalysis(n, scope, errors));

            default -> errors.add("Unsupported node: " + node.getClass());
        };
    }

    private static void typeCheckCommand(Node command, Scope scope, List<String> errors) {
        switch (command) {
            case Node.InterrompaCommandNode interrompaCommandNode -> {
                if (scope.parent() == null) {
                    errors.add("Interrompa command outside of a function or procedure");
                }
            }
            case Node.ReturnNode(var node, var location) -> {
                if (scope.parent() == null) {
                    errors.add("Return command outside of a function");
                }
                Type type = getType(node, scope, errors);
                Optional<Declaration.Function> function = scope.function(scope.name);
                if (function.isEmpty()) {
                    errors.add("Should have a function");

                } else {
                    if (!areTypesCompatible(type, function.get().returnType())) {
                        errors.add("Return type different from function type: " + function.get().returnType() + " and " + type);
                    }
                }

            }

            case Node.DosNode dosNode -> {
                if (scope.parent() != null) {
                    errors.add("Dos command inside a function or procedure");
                }
            }

            case Node.IncrementNode incrementNode -> {
                Type type = getType(incrementNode.expr(), scope, errors);
                if (type instanceof Type.PrimitiveTypes) {
                    errors.add("Increment command on a primitive type: " + type);
                }
            }

            case Node.AssignmentNode assignmentNode -> {
                Type idType = getType(assignmentNode.idOrArray(), scope, errors);
                Type exprType = getType(assignmentNode.expr(), scope, errors);

                if (idType instanceof Type.Array arrayType) {
                    if (!arrayType.type().equals(exprType)) {
                        errors.add("Assignment of different types: " + arrayType.type() + " and " + exprType);
                    }
                } else if (!areTypesCompatible(idType, exprType)) {
                     errors.add(assignmentNode.location() + ": Assignment of different types: " + idType + " and " + exprType);
                }
            }

            case Node.ForCommandNode forCommandNode -> {
                Node.IdNode identifier = forCommandNode.identifier();
                if (scope.variable(identifier.id()).isEmpty()) {
                    errors.add("For command with undeclared variable: " + identifier.id());
                }

                Type startType = getType(forCommandNode.startValue(), scope, errors);
                Type endType = getType(forCommandNode.endValue(), scope, errors);
                Type stepType = getType(forCommandNode.step(), scope, errors);
                if (startType != Type.PrimitiveTypes.INTEIRO) {
                    errors.add("For start command with non-integer types: " + startType);
                }
                if (endType != Type.PrimitiveTypes.INTEIRO && endType != Type.PrimitiveTypes.UNDEFINED) {
                    errors.add("For end command with non-integer types: " + endType);
                }
                if (stepType != Type.PrimitiveTypes.INTEIRO && stepType != Type.PrimitiveTypes.UNDECLARED) {
                    errors.add("For step command with non-integer types: " + stepType);
                }
                forCommandNode.commands().forEach(x -> typeCheckCommand(x, scope, errors));
            }

            case Node.WhileCommandNode whileCommandNode -> {
                Type testType = getType(whileCommandNode.test(), scope, errors);
                if (testType != Type.PrimitiveTypes.LOGICO) {
                    errors.add("While command with non-boolean type: " + testType);
                }
                whileCommandNode.commands().forEach(x -> typeCheckCommand(x, scope, errors));
            }

            case Node.ArrayAccessNode arrayAccessNode -> {
                Type idType = getType(arrayAccessNode.node(), scope, errors);
                if (!(idType instanceof Type.Array)) {
                    errors.add("Array access on a non-array type:" + idType);
                }
                for (Node index : arrayAccessNode.indexes()) {
                    Type indexType = getType(index, scope, errors);
                    if (indexType != Type.PrimitiveTypes.INTEIRO) {
                        errors.add("Array access with non-integer index: " + indexType);
                    }
                }
            }

            case Node.IdNode idNode -> {
                if (scope.declaration(idNode.id()).isEmpty()) {
                    errors.add(idNode.id() + " not declared");
                }
            }

            case Node.DivNode(Node left, Node right, Location location) -> {
                Type leftType = getType(left, scope, errors);
                Type rightType = getType(right, scope, errors);
                if (leftType != Type.PrimitiveTypes.INTEIRO || rightType != Type.PrimitiveTypes.INTEIRO) {
                    errors.add("Div command with non-integer types: " + leftType + " and " + rightType);
                }
            }

            case Node.ModNode(Node left, Node right, Location location) -> {
                Type leftType = getType(left, scope, errors);
                Type rightType = getType(right, scope, errors);
                if (leftType != Type.PrimitiveTypes.INTEIRO || rightType != Type.PrimitiveTypes.INTEIRO) {
                    errors.add("Mod command with non-integer types: " + leftType + " and " + rightType);
                }
            }

            case Node.AddNode(Node left, Node right, Location location) -> {
                Type leftType = getType(left, scope, errors);
                Type rightType = getType(right, scope, errors);
                if (leftType != rightType) {
                    errors.add("Add command with different types: " + leftType + " and " + rightType);
                } else if (leftType != Type.PrimitiveTypes.INTEIRO && leftType != Type.PrimitiveTypes.REAL && leftType != CARACTERE) {
                    errors.add("Add command with non-integer, real or character types: " + leftType);
                }

            }

            case Node.SubNode(Node left, Node right, Location location) -> {
                Type leftType = getType(left, scope, errors);
                Type rightType = getType(right, scope, errors);
                if (!areNumbers(leftType, rightType)) {
                    errors.add("Sub command with non-number types: " + leftType + " and " + rightType);
                }
            }

            case Node.MulNode(Node left, Node right, Location location) -> {
                Type leftType = getType(left, scope, errors);
                Type rightType = getType(right, scope, errors);
                if (!areNumbers(leftType, rightType)) {
                    errors.add("Mul command with non-number types: " + leftType + " and " + rightType);
                }
            }

            case Node.PowNode(Node left, Node right, Location location) -> {
                Type leftType = getType(left, scope, errors);
                Type rightType = getType(right, scope, errors);
                if (!areNumbers(leftType, rightType)) {
                    errors.add("Pow command with non-number types: " + leftType + " and " + rightType);
                }
            }

            case Node.AndNode(Node left, Node right, Location location) -> {
                Type leftType = getType(left, scope, errors);
                Type rightType = getType(right, scope, errors);
                if (!areThey(LOGICO, leftType, rightType)) {
                    errors.add("And command with non-boolean types: " + leftType + " and " + rightType);
                }
            }

            case Node.OrNode(Node left, Node right, Location location) -> {
                Type leftType = getType(left, scope, errors);
                Type rightType = getType(right, scope, errors);
                if (!areThey(LOGICO, leftType, rightType)) {
                    errors.add("Or command with non-boolean types: " + leftType + " and " + rightType);
                }
            }

            case Node.NotNode(Node expr, Location location) -> {
                Type exprType = getType(expr, scope, errors);
                if (exprType != Type.PrimitiveTypes.LOGICO) {
                    errors.add("Not command with non-boolean type: " + exprType);
                }
            }

            case Node.EqNode(Node left, Node right, Location location) -> {
                Type leftType = getType(left, scope, errors);
                Type rightType = getType(right, scope, errors);
                if (!areTypesCompatible(leftType, rightType)) {
                    errors.add("Eq command with different types: " + leftType + " and " + rightType);
                }
            }

            case Node.NeNode(Node left, Node right, Location location) -> {
                Type leftType = getType(left, scope, errors);
                Type rightType = getType(right, scope, errors);
                if (!areTypesCompatible(leftType, rightType)) {
                    errors.add("Ne command with different types: " + leftType + " and " + rightType);
                }
            }

            case Node.LtNode(Node left, Node right, Location location) -> {
                Type leftType = getType(left, scope, errors);
                Type rightType = getType(right, scope, errors);
                if (!areTypesCompatible(leftType, rightType)) {
                    errors.add("Lt command with different types: " + leftType + " and " + rightType);
                }
            }

            case Node.LeNode(Node left, Node right, Location location) -> {
                Type leftType = getType(left, scope, errors);
                Type rightType = getType(right, scope, errors);
                if (!areTypesCompatible(leftType, rightType)) {
                    errors.add("Le command with different types: " + leftType + " and " + rightType);
                }
            }

            case Node.GtNode(Node left, Node right, Location location) -> {
                Type leftType = getType(left, scope, errors);
                Type rightType = getType(right, scope, errors);
                if (!areTypesCompatible(leftType, rightType)) {
                    errors.add("Gt command with different types: " + leftType + " and " + rightType);
                }
            }

            case Node.GeNode(Node left, Node right, Location location) -> {
                Type leftType = getType(left, scope, errors);
                Type rightType = getType(right, scope, errors);
                if (!areTypesCompatible(leftType, rightType)) {
                    errors.add("Ge command with different types: " + leftType + " and " + rightType);
                }
            }

            case Node.NegNode(Node expr, Location location) -> {
                Type exprType = getType(expr, scope, errors);
                if (!areNumbers(exprType, Type.PrimitiveTypes.REAL)) {
                    errors.add("Neg command with non number type: " + exprType);
                }
            }

            case Node.ConditionalCommandNode ifNode -> {
                Type conditionType = getType(ifNode.expr(), scope, errors);
                if (conditionType != Type.PrimitiveTypes.LOGICO) {
                    errors.add("If command with non-boolean condition:" + conditionType);
                }
                ifNode.commands().forEach(c -> typeCheckCommand(c, scope, errors));
                ifNode.elseCommands().forEach(c -> typeCheckCommand(c, scope, errors));
            }

            case Node.ProcedureCallNode procedureCallNode -> {
                if (scope.procedure(procedureCallNode.name()).isEmpty()) {
                    errors.add("Procedure " + procedureCallNode.name() + " not declared");
                }
                for (Node arg : procedureCallNode.args()) {
                    Type argType = getType(arg, scope, errors);
                    if (argType == Type.PrimitiveTypes.UNDECLARED) {
                        errors.add("Procedure call with undeclared type");
                    }
                }
            }

            case Node.FunctionCallNode functionCallNode -> {
                if (scope.function(functionCallNode.name()).isEmpty()) {
                    errors.add("Function " + functionCallNode.name() + " not declared");
                }
                for (Node arg : functionCallNode.args()) {
                    Type argType = getType(arg, scope, errors);
                    if (argType == Type.PrimitiveTypes.UNDECLARED) {
                        errors.add("Function call with undeclared type");
                    }
                }
            }

            case Node.AleatorioCommandNode aleatorioCommandNode -> {
                for (Integer arg : aleatorioCommandNode.args()) {
                    if (arg < 0) {
                        errors.add("Aleatorio command with negative argument: " + arg);
                    }
                }
            }

            case Node.TimerCommandNode timerCommandNode -> {
                if (timerCommandNode.value() < 0) {
                    errors.add("Timer command with negative argument: " + timerCommandNode.value());
                }
            }

            case Node.ChooseCommandNode commandNode -> {
                Type type = getType(commandNode.expr(), scope, errors);
                if (type != Type.PrimitiveTypes.INTEIRO && type != CARACTERE) {
                    errors.add("Choose command with non-integer type: " + type);
                }
                commandNode.cases().forEach(c -> typeCheckCommand(c, scope, errors));
            }

            case Node.ChooseCaseNode caseNode -> {
                Type type = getType(caseNode.value(), scope, errors);
                if (!areNumbers(type, REAL) && type != CARACTERE) {
                    errors.add("Choose case with non number neither caracter type: " + type);
                }
                caseNode.commands().forEach(c -> typeCheckCommand(c, scope, errors));
            }

            case Node.ReadCommandNode readCommandNode -> {
                for (Node id : readCommandNode.exprList()) {
                    Type idType = getType(id, scope, errors);
                    if (idType == Type.PrimitiveTypes.UNDECLARED) {
                        errors.add("Read command on undeclared type");
                    } else if (idType instanceof Type.Array) {
                        errors.add("Read command on array type: " + idType);
                    }
                }
            }

            case Node.WriteItemNode writeItemNode -> {
                Type type = getType(writeItemNode.expr(), scope, errors);
                if (type == Type.PrimitiveTypes.UNDECLARED) {
                    errors.add("Write command on undeclared type");
                }
                if (writeItemNode.precision() != null && type != Type.PrimitiveTypes.REAL) {
                    errors.add("Write command with precision on non-real type: " + type);
                }
            }
            case Node.WriteCommandNode writeCommandNode -> writeCommandNode.writeList().forEach(x -> semanticAnalysis(x, scope, errors));
            case Node.LimpatelaCommandNode _, Node.EcoCommandNode _, Node.DebugCommandNode _, Node.PausaCommandNode _, Node.CronometroCommandNode _ -> {}
            case Node.CommandNode commandNode -> typeCheckCommand(commandNode.command(), scope, errors);
            default -> errors.add("Unsupported command node: " + command.getClass());
        }
    }

    private static Type getType(Node node, Scope scope, List<String> errors) {
        return switch (node) {
            case null -> Type.PrimitiveTypes.UNDEFINED;
            case Node.TypeNode typeNode ->
                switch (typeNode.type().toLowerCase()) {
                    case "inteiro" -> Type.PrimitiveTypes.INTEIRO;
                    case "real", "numerico" -> Type.PrimitiveTypes.REAL;
                    case "logico" -> Type.PrimitiveTypes.LOGICO;
                    case "caractere", "caracter", "literal" -> CARACTERE;
                    default -> {
                        Optional<Declaration.UserDefinedType> type = scope.type(typeNode.type());
                        if (type.isPresent()) {
                            yield type.get();
                        } else {
                            errors.add("Type " + typeNode.type() + " not declared");
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
                        errors.add("Member access with non-id node: " + member);
                        yield Type.PrimitiveTypes.UNDECLARED;
                    }
                    Declaration.Variable variable = userDefinedType.variables().get(idNode.id().toLowerCase());
                    if (variable == null) {
                        errors.add("Member " + member + " not declared in type " + userDefinedType.name());
                        yield Type.PrimitiveTypes.UNDECLARED;
                    }
                    yield variable.type();
                } else {
                    errors.add("Member access on a non-user defined type: " + idType);
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
                    errors.add(idNode.id() + " not declared");
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
                yield getType(declaration.get(), scope, errors);
            }

            case Node.BinaryNode binaryNode -> getType(binaryNode, scope, errors);

            case Node.NotNode notNode -> {
                Type exprType = getType(notNode.expr(), scope, errors);
                if (exprType == Type.PrimitiveTypes.LOGICO) {
                    yield Type.PrimitiveTypes.LOGICO;
                } else {
                    errors.add("Not operation with non-boolean type: " + exprType);
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
            }

            case Node.ProcedureCallNode procedureCallNode -> {
                if (scope.procedure(procedureCallNode.name()).isPresent()) {
                    errors.add("Procedure " + procedureCallNode.name() + " does not return a value");
                }
                yield Type.PrimitiveTypes.UNDECLARED;
            }
            case Node.FunctionCallNode functionCallNode -> {
                Optional<Declaration.Function> function = scope.function(functionCallNode.name());
                if (function.isEmpty()) {
                    errors.add("Function " + functionCallNode.name() + " not declared");
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
                yield function.get().returnType();
            }

            case Node.CompundNode compundNode -> {
                yield null;
            }

            case Node.RangeNode(Node start, Node end, _) -> {
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
                    errors.add("Binary operation with non-primitive types: " + startType + " and " + endType);
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
            }

            default -> {
                errors.add("Unsupported type node: " + node.getClass());
                yield Type.PrimitiveTypes.UNDECLARED;
            }

        };
    }

    private static Type getType(Declaration declaration, Scope scope, List<String> errors) {
        return switch (declaration) {
            case Declaration.Variable variable -> variable.type();
            case Declaration.Constant constant -> constant.type();
            case Declaration.Function function -> function.returnType();
            case Declaration.Procedure procedure -> Type.PrimitiveTypes.UNDECLARED;
            case Declaration.UserDefinedType userDefinedType -> userDefinedType;
        };
    }

    private static Type getType(Node.BinaryNode node, Scope scope, List<String> errors) {
        return switch (node) {
            case Node.AddNode _ -> {
                Type leftType = getType(node.left(), scope, errors);
                Type rightType = getType(node.right(), scope, errors);
                if (areThey(CARACTERE, leftType, rightType)) {
                    yield CARACTERE;
                } else if (areNumbers(leftType, rightType)) {
                    yield getGeneralNumberType(leftType, rightType);
                }else {
                    errors.add("Binary operation with non-primitive types: " + leftType + " and " + rightType);
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
            }

            case Node.SubNode _, Node.MulNode _, Node.DivNode _, Node.ModNode _, Node.PowNode _ -> {
                Type leftType = getType(node.left(), scope, errors);
                Type rightType = getType(node.right(), scope, errors);
                if (areNumbers(leftType, rightType)) {
                    yield getGeneralNumberType(leftType, rightType);
                } else {
                    errors.add("Binary operation with non-primitive types: " + leftType + " and " + rightType);
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
            }
            case Node.AndNode _, Node.OrNode _ -> {
                Type leftType = getType(node.left(), scope, errors);
                Type rightType = getType(node.right(), scope, errors);
                if (areThey(Type.PrimitiveTypes.LOGICO, leftType, rightType)) {
                    yield Type.PrimitiveTypes.LOGICO;
                } else {
                    errors.add("Binary operation with non-boolean types: " + leftType + " and " + rightType);
                    yield Type.PrimitiveTypes.UNDECLARED;
                }
            }
            case Node.EqNode _, Node.NeNode _, Node.LtNode _, Node.LeNode _, Node.GtNode _, Node.GeNode _ -> {
                Type leftType = getType(node.left(), scope, errors);
                Type rightType = getType(node.right(), scope, errors);
                if (areTypesCompatible(leftType, rightType)) {
                    yield Type.PrimitiveTypes.LOGICO;
                } else {
                    errors.add("Binary operation with different types: " + leftType + " and " + rightType);
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