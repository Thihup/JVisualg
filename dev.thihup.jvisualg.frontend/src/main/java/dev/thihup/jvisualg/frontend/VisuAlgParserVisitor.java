package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgParser;
import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgParserBaseVisitor;
import dev.thihup.jvisualg.frontend.node.Location;
import dev.thihup.jvisualg.frontend.node.Node;
import dev.thihup.jvisualg.frontend.node.Node.*;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jspecify.annotations.NullUnmarked;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;

class VisuAlgParserVisitor extends VisuAlgParserBaseVisitor<Node> {

    private static Optional<Location> fromRuleContext(ParserRuleContext ctx) {
        return Optional.of(new Location(
                ctx.start.getLine(),
                ctx.start.getCharPositionInLine(),
                ctx.stop.getLine(),
                ctx.stop.getCharPositionInLine() + ctx.stop.getText().length() - 1
        ));
    }

    private static Optional<Location> fromTerminalNode(TerminalNode ctx) {
        return Optional.of(new Location(
                ctx.getSymbol().getLine(),
                ctx.getSymbol().getCharPositionInLine(),
                ctx.getSymbol().getLine(),
                ctx.getSymbol().getCharPositionInLine() + ctx.getSymbol().getText().length() - 1
        ));
    }

    @Override
    public Node visitAlgorithm(VisuAlgParser.AlgorithmContext ctx) {
        return new AlgoritimoNode(new StringLiteralNode(ctx.STRING().getText(), fromTerminalNode(ctx.STRING())), visit(ctx.declarations()), visit(ctx.commands()), fromRuleContext(ctx));
    }

    @Override
    public Node visitDeclarations(VisuAlgParser.DeclarationsContext ctx) {
        if (ctx.children == null) {
            return CompundNode.empty();
        }
        List<Node> list = ctx.children.stream()
            .map(this::visit)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(x -> x instanceof SubprogramDeclarationNode))
            .toList();
        return new CompundNode<>(list, fromRuleContext(ctx));
    }


    @Override
    public Node visitSubprogramDeclaration(VisuAlgParser.SubprogramDeclarationContext ctx) {
        VisuAlgParser.FunctionDeclarationContext functionDeclarationContext = ctx.functionDeclaration();
        if (functionDeclarationContext != null) {
            return visit(functionDeclarationContext);
        }
        if (ctx.procedureDeclaration() != null) {
            return visit(ctx.procedureDeclaration());
        }
        throw new UnsupportedOperationException();
    }

    @Override
    public CompundNode<VariableDeclarationNode> visitFormalParameter(VisuAlgParser.FormalParameterContext ctx) {
        return new CompundNode<>(ctx.ID().stream().map(x -> new VariableDeclarationNode(visitId(x), visitType(ctx.type()), ctx.VAR() != null, fromRuleContext(ctx))).toList(), fromRuleContext(ctx));
    }

    @Override
    public Node visitProcedureDeclaration(VisuAlgParser.ProcedureDeclarationContext ctx) {
        CompundNode<VariableDeclarationNode> parameters;
        if (ctx.formalParameterList() instanceof VisuAlgParser.FormalParameterListContext context) {
            parameters = context.formalParameter().stream().map(this::visitFormalParameter).mapMulti((CompundNode<VariableDeclarationNode> a, Consumer<VariableDeclarationNode> b) -> {
                a.nodes().forEach(b);
            }).collect(toCompundNode(fromRuleContext(context)));

        } else {
            parameters = CompundNode.empty();
        }
        CompundNode<Node> declarations = ctx.declarations() instanceof VisuAlgParser.DeclarationsContext context ? context.variableDeclaration().stream().map(this::visit).collect(toCompundNode(fromRuleContext(context))) : CompundNode.empty();
        CompundNode<CommandNode> commands = ctx.commands() instanceof VisuAlgParser.CommandsContext context ? context.command().stream().map(this::visitCommand).collect(toCompundNode(fromRuleContext(context))) : CompundNode.empty();
        return new ProcedureDeclarationNode(
            visitId(ctx.ID()),
                parameters,
                declarations,
                commands,
            fromRuleContext(ctx)
        );
    }

    @Override
    public Node visitFunctionDeclaration(VisuAlgParser.FunctionDeclarationContext ctx) {
        CompundNode<VariableDeclarationNode> parameters;
        if (ctx.formalParameterList() instanceof VisuAlgParser.FormalParameterListContext context) {
            parameters = context.formalParameter().stream().map(this::visitFormalParameter).mapMulti((CompundNode<VariableDeclarationNode> a, Consumer<VariableDeclarationNode> b) -> {
                a.nodes().forEach(b);
            }).collect(toCompundNode(fromRuleContext(context)));
        } else {
            parameters = CompundNode.empty();
        }
        CompundNode<Node> declarations = ctx.declarations() instanceof VisuAlgParser.DeclarationsContext context ? context.variableDeclaration().stream().map(this::visit).collect(toCompundNode(fromRuleContext(context))) : CompundNode.empty();
        CompundNode<CommandNode> commands = ctx.commands() instanceof VisuAlgParser.CommandsContext context ? context.command().stream().map(this::visitCommand).collect(toCompundNode(fromRuleContext(context))) : CompundNode.empty();
        return new FunctionDeclarationNode(
                visitId(ctx.ID()),
                visitType(ctx.type()),
                parameters,
                declarations,
                commands,
                fromRuleContext(ctx)
        );
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private <T extends Node> Collector<T, ?, CompundNode<T>> toCompundNode(Optional<Location> location) {
        return Collectors.collectingAndThen(Collectors.toUnmodifiableList(), node -> new CompundNode<>(node, location));
    }

    @Override
    public Node visitRegistroDeclaration(VisuAlgParser.RegistroDeclarationContext ctx) {
        CompundNode<VariableDeclarationNode> variableDeclarationContexts = ctx.variableDeclaration().stream()
                .flatMap((VisuAlgParser.VariableDeclarationContext ctx1) -> visitVariableDeclaration(ctx1).nodes().stream())
                .collect(toCompundNode(fromRuleContext(ctx)));
        return new RegistroDeclarationNode(visitId(ctx.ID()), variableDeclarationContexts, fromRuleContext(ctx));
    }


    @Override
    public CompundNode<VariableDeclarationNode> visitVariableDeclaration(VisuAlgParser.VariableDeclarationContext ctx) {
        return ctx.ID().stream()
                .map(x -> new VariableDeclarationNode(visitId(x), visitType(ctx.type()), false, fromRuleContext(ctx)))
                .collect(toCompundNode(fromRuleContext(ctx)));
    }


    @Override
    public Node visitConstantDeclaration(VisuAlgParser.ConstantDeclarationContext ctx) {
        return new ConstantNode(visitId(ctx.ID()), visitExpr(ctx.expr()), fromRuleContext(ctx));
    }


    @Override
    public ExpressionNode visitLiterais(VisuAlgParser.LiteraisContext ctx) {
        TerminalNode falso = ctx.FALSO();
        if (falso != null) {
            return new BooleanLiteralNode(false, fromTerminalNode(falso));
        }
        TerminalNode verdadeiro = ctx.VERDADEIRO();
        if (verdadeiro != null) {
            return new BooleanLiteralNode(true, fromTerminalNode(verdadeiro));
        }
        TerminalNode string = ctx.STRING();
        if (string != null) {
            return new StringLiteralNode(string.getText().substring(1, string.getText().length() - 1), fromTerminalNode(string));
        }
        TerminalNode real = ctx.REAL_LITERAL();
        if (real != null) {
            return new RealLiteralNode(Double.parseDouble(real.getText()), fromTerminalNode(real));
        }
        TerminalNode inteiro = ctx.INT_LITERAL();
        if (inteiro != null) {
            return new IntLiteralNode(Integer.parseInt(inteiro.getText()), fromTerminalNode(inteiro));
        }
        throw new RuntimeException("Unknown literal: " + ctx.getText());
    }


    @Override
    public TypeNode visitType(VisuAlgParser.TypeContext ctx) {
        TerminalNode vetor = ctx.VETOR();
        Optional<Location> location = fromRuleContext(ctx);
        if (vetor == null) {
            return switch (ctx.getText().toLowerCase()) {
                case "inteiro" -> new InteiroType(location);
                case "real", "numerico" -> new RealType(location);
                case "logico" -> new LogicoType(location);
                case "caractere", "caracter", "literal" -> new CaracterType(location);
                default -> new UserDefinedType(new StringLiteralNode(ctx.getText(), location), location);
            };
        }
        List<TerminalNode> range = ctx.RANGE();

        CompundNode<RangeNode> list = range.stream()
                .map(this::rangeToNode)
                .collect(toCompundNode(location));
        return new ArrayTypeNode(visitType(ctx.type()), list, location);
    }

    private RangeNode rangeToNode(TerminalNode ctx) {
        String[] values = ctx.getText().split("\\.\\.");
        assert values.length == 2;
        ExpressionNode min, max;
        try {
            min = new IntLiteralNode(Integer.parseInt(values[0]), fromTerminalNode(ctx));
        } catch (NumberFormatException e) {
            min = new IdNode(values[0], fromTerminalNode(ctx));
        }
        try {
            max = new IntLiteralNode(Integer.parseInt(values[1]), fromTerminalNode(ctx));
        } catch (NumberFormatException e) {
            max = new IdNode(values[1], fromTerminalNode(ctx));
        }

        return new RangeNode(
                min,
                max,
                fromTerminalNode(ctx)
        );
    }

    @Override
    public Node visitCommands(VisuAlgParser.CommandsContext ctx) {
        List<Node> commands = ctx.command().stream().map(this::visit).toList();
        return new CompundNode<>(commands, fromRuleContext(ctx));
    }


    @Override
    public Node.CommandNode visitCommand(VisuAlgParser.CommandContext ctx) {
        return (Node.CommandNode) visit(ctx.getChild(0));
    }


    @Override
    public Node visitFimAlgoritmo(VisuAlgParser.FimAlgoritmoContext ctx) {
        return new EndAlgorithmCommand(fromRuleContext(ctx));
    }

    @Override
    public Node visitInterrompaCommand(VisuAlgParser.InterrompaCommandContext ctx) {
        return new InterrompaCommandNode(fromRuleContext(ctx));
    }


    @Override
    public Node visitReturn(VisuAlgParser.ReturnContext ctx) {
        ExpressionNode expr = ctx.expr() instanceof VisuAlgParser.ExprContext e? visitExpr(e) : EmptyExpressionNode.INSTANCE;
        return new ReturnNode(expr, fromRuleContext(ctx));
    }


    @Override
    public Node visitDos(VisuAlgParser.DosContext ctx) {
        return new DosNode(fromRuleContext(ctx));
    }


    @Override
    public Node visitAssignment(VisuAlgParser.AssignmentContext ctx) {
        return new AssignmentNode(visit(ctx.idOrArray()), visitExpr(ctx.expr()), fromRuleContext(ctx));
    }


    @Override
    public ExpressionNode visitIdOrArray(VisuAlgParser.IdOrArrayContext ctx) {
        return ctx.children
            .stream()
            .map(this::visit)
            .filter(x -> x instanceof ExpressionNode)
            .map(x -> (ExpressionNode) x)
            .reduce((result, element) -> switch (element) {
                case ArrayAccessNode(_, CompundNode<ExpressionNode> indexes, var location) ->
                        new ArrayAccessNode(result, indexes, location);
                case MemberAccessNode(_, Node expr, var location) ->
                        new MemberAccessNode(result, expr, location);
                case IdNode id -> new IdNode(id.id(), fromRuleContext(ctx));
                default -> throw new IllegalStateException();
            })
        .orElseThrow();
    }

    @Override
    public Node visitMemberAccess(VisuAlgParser.MemberAccessContext ctx) {
        return new MemberAccessNode(EmptyExpressionNode.INSTANCE, visit(ctx.idOrArray()), fromRuleContext(ctx));
    }

    @Override
    public Node visitArrayAccess(VisuAlgParser.ArrayAccessContext ctx) {
        return new ArrayAccessNode(EmptyNode.INSTANCE, ctx.expr().stream().map(this::visitExpr).collect(toCompundNode(fromRuleContext(ctx))), fromRuleContext(ctx));
    }

    @Override
    public IdNode visitId(VisuAlgParser.IdContext ctx) {
        return new IdNode(ctx.ID().getText(), fromRuleContext(ctx));
    }

    private IdNode visitId(TerminalNode ctx) {
        return new IdNode(ctx.getText(), fromTerminalNode(ctx));
    }

    @Override
    public Node visitReadCommand(VisuAlgParser.ReadCommandContext ctx) {
        CompundNode<ExpressionNode> leia = ctx.exprList()
                .expr()
                .stream()
                .map(this::visitExpr)
                .collect(toCompundNode(fromRuleContext(ctx)));

        return new ReadCommandNode(leia, fromRuleContext(ctx));
    }




    @Override
    public Node visitWriteCommand(VisuAlgParser.WriteCommandContext ctx) {
        boolean newLine = ctx.ESCREVAL() != null;
        VisuAlgParser.WriteListContext writeListContext = ctx.writeList();
        if (writeListContext == null) {
            return new WriteCommandNode(newLine, CompundNode.empty(), fromRuleContext(ctx));
        }
        CompundNode<WriteItemNode> writeList = writeListContext.writeItem().stream().map(this::visitWriteItem).collect(toCompundNode(fromRuleContext(ctx)));
        return new WriteCommandNode(newLine, writeList, fromRuleContext(ctx));
    }


    @Override
    public WriteItemNode visitWriteItem(VisuAlgParser.WriteItemContext ctx) {
        ExpressionNode expr = visitExpr(ctx.expr(0));
        Node spaces = ctx.expr(1) != null ? visit(ctx.expr(1)) : EmptyNode.INSTANCE;
        Node precision = ctx.expr(2) != null ? visit(ctx.expr(2)) : EmptyNode.INSTANCE;

        return new WriteItemNode(expr, spaces, precision, fromRuleContext(ctx));
    }


    @Override
    public Node visitConditionalCommand(VisuAlgParser.ConditionalCommandContext ctx) {
        ExpressionNode expr = visitExpr(ctx.expr());
        CompundNode<CommandNode> commands = ctx.commands(0).command().stream().map(this::visitCommand).collect(toCompundNode(fromRuleContext(ctx)));
        VisuAlgParser.CommandsContext commands1 = ctx.commands(1);
        if (commands1 == null || commands1.command().isEmpty()) {
            return new ConditionalCommandNode(expr, commands, CompundNode.empty(), fromRuleContext(ctx));
        }
        CompundNode<CommandNode> elseCommands = commands1.command().stream().map(this::visitCommand).collect(toCompundNode(fromRuleContext(ctx)));
        return new ConditionalCommandNode(expr, commands, elseCommands, fromRuleContext(ctx));
    }


    @Override
    public ChooseCommandNode visitChooseCommand(VisuAlgParser.ChooseCommandContext ctx) {
        ExpressionNode expr = visitExpr(ctx.expr());
        CompundNode<ChooseCaseNode> cases = ctx.chooseCase().stream().map(this::visitChooseCase).collect(toCompundNode(fromRuleContext(ctx)));
        VisuAlgParser.OutroCaseContext outroCaseContext = ctx.outroCase();
        ChooseCaseNode defaultCase = outroCaseContext != null ? visitOutroCase(outroCaseContext) : ChooseCaseNode.EMPTY;
        return new ChooseCommandNode(expr, cases, defaultCase, fromRuleContext(ctx));
    }

    @Override
    public ChooseCaseNode visitChooseCase(VisuAlgParser.ChooseCaseContext ctx) {
        CompundNode<ExpressionNode> expr = visitExprOrAteList(ctx.exprOrAteList());
        CompundNode<CommandNode> commands = ctx.commands().command().stream().map(this::visitCommand).collect(toCompundNode(fromRuleContext(ctx)));
        return new ChooseCaseNode(expr, commands, fromRuleContext(ctx));
    }

    @Override
    public CompundNode<ExpressionNode> visitExprOrAteList(VisuAlgParser.ExprOrAteListContext ctx) {
        return ctx.exprOrAte().stream().map(this::visit).map(x -> (ExpressionNode) x).collect(toCompundNode(fromRuleContext(ctx)));
    }

    @Override
    public ChooseCaseNode visitOutroCase(VisuAlgParser.OutroCaseContext ctx) {
        CompundNode<CommandNode> commands = ctx.commands().command().stream().map(this::visitCommand).collect(toCompundNode(fromRuleContext(ctx)));
        return new ChooseCaseNode(CompundNode.empty(), commands, fromRuleContext(ctx));
    }

    @Override
    public ExpressionNode visitExprOrAte(VisuAlgParser.ExprOrAteContext ctx) {
        if (ctx.ATE() != null) {
            return new RangeNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)),fromRuleContext(ctx));
        }
        return visitExpr(ctx.expr(0));
    }

    @Override
    public Node visitParaCommand(VisuAlgParser.ParaCommandContext ctx) {
        IdNode idOrArray = visitId(ctx.ID());
        ExpressionNode endValue = ctx.expr(1) instanceof VisuAlgParser.ExprContext endValueExpr ? visitExpr(endValueExpr) : EmptyExpressionNode.INSTANCE;
        ExpressionNode step = ctx.expr(2) instanceof VisuAlgParser.ExprContext stepValue ? visitExpr(stepValue) : new IntLiteralNode(1, Optional.empty());
        CompundNode<CommandNode> commands = ctx.commands().command().stream().map(this::visitCommand).collect(toCompundNode(fromRuleContext(ctx)));


        return new ForCommandNode(
            idOrArray,
            visitExpr(ctx.expr(0)),
            endValue,
            step,
            commands,
            fromRuleContext(ctx));
    }

    @Override
    public ExpressionNode visitExpr(VisuAlgParser.ExprContext ctx) {
        if (ctx.atom() instanceof VisuAlgParser.AtomContext atomContext) {
            return visitAtom(atomContext);
        }
        if (ctx.parenExpression() instanceof VisuAlgParser.ParenExpressionContext parenExpressionContext) {
            return visitExpr(parenExpressionContext.expr());
        }
        if (ctx.SUB() != null && ctx.expr().size() == 1) {
            return new NegNode(visitExpr(ctx.expr(0)), fromRuleContext(ctx));
        }
        if (ctx.NOT() != null) {
            return new NotNode(visitExpr(ctx.expr(0)), fromRuleContext(ctx));
        }
        String text = ctx.getChild(1).getText();
        return switch (text.toLowerCase()) {
            case "+" -> new AddNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), fromRuleContext(ctx));
            case "-" -> new SubNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), fromRuleContext(ctx));
            case "*" -> new MulNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), fromRuleContext(ctx));
            case "/", "div" -> new DivNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), false, fromRuleContext(ctx));
            case "\\" -> new DivNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), true, fromRuleContext(ctx));
            case "e" -> new AndNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), fromRuleContext(ctx));
            case "ou" -> new OrNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), fromRuleContext(ctx));
            case "nao", "não" -> new NotNode(visitExpr(ctx.expr(0)), fromRuleContext(ctx));
            case "=" -> new EqNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), fromRuleContext(ctx));
            case "<" -> new LtNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), fromRuleContext(ctx));
            case ">" -> new GtNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), fromRuleContext(ctx));
            case "<=" -> new LeNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), fromRuleContext(ctx));
            case ">=" -> new GeNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), fromRuleContext(ctx));
            case "<>" -> new NeNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), fromRuleContext(ctx));
            case "%", "mod" -> new ModNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), fromRuleContext(ctx));
            case "^" -> new PowNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), fromRuleContext(ctx));
            default -> throw new UnsupportedOperationException(text + " -> " + ctx.getParent().getText());
        };
    }

    @Override
    public ExpressionNode visitAtom(VisuAlgParser.AtomContext ctx) {
        if (ctx.idOrArray() != null) {
            return visitIdOrArray(ctx.idOrArray());
        }
        if (ctx.literais() != null) {
            return visitLiterais(ctx.literais());
        }
        if (ctx.functionCall() != null) {
            return visitFunctionCall(ctx.functionCall());
        }
        throw new UnsupportedOperationException(ctx.getText());
    }



    @Override
    public Node visitEnquantoCommand(VisuAlgParser.EnquantoCommandContext ctx) {
        ExpressionNode test = visitExpr(ctx.expr());
        CompundNode<CommandNode> commands = ctx.commands().command().stream().map(this::visitCommand).collect(toCompundNode(fromRuleContext(ctx)));
        return new WhileCommandNode(test, commands, false, fromRuleContext(ctx));
    }

    @Override
    public Node visitRepitaCommand(VisuAlgParser.RepitaCommandContext ctx) {
        ExpressionNode test = ctx.expr() instanceof VisuAlgParser.ExprContext v ? visitExpr(v) : new BooleanLiteralNode(false, fromRuleContext(ctx));
        CompundNode<CommandNode> commands = ctx.commands().command().stream().map(this::visitCommand).collect(toCompundNode(fromRuleContext(ctx)));
        return new WhileCommandNode(test, commands, true, fromRuleContext(ctx));
    }


    @Override
    public Node visitProcedureCall(VisuAlgParser.ProcedureCallContext ctx) {
        VisuAlgParser.ExprListContext exprListContext = ctx.exprList();
        IdNode name = visitId(ctx.ID());
        if (exprListContext == null) {
            return new ProcedureCallNode(name, CompundNode.empty(), fromRuleContext(ctx));
        }
        CompundNode<ExpressionNode> args = exprListContext.expr().stream().map(this::visitExpr).collect(toCompundNode(fromRuleContext(ctx)));
        return new ProcedureCallNode(name, args, fromRuleContext(ctx));
    }

    @Override
    public ExpressionNode visitFunctionCall(VisuAlgParser.FunctionCallContext ctx) {
        IdNode name = visitId(ctx.ID());
        VisuAlgParser.ExprListContext exprListContext = ctx.exprList();
        if (exprListContext == null) {
            return new FunctionCallNode(name, CompundNode.empty(), fromRuleContext(ctx));
        }
        CompundNode<ExpressionNode> args = exprListContext.expr().stream().map(this::visitExpr).collect(toCompundNode(fromRuleContext(ctx)));
        return new FunctionCallNode(name, args, fromRuleContext(ctx));
    }


    @Override
    public Node visitArquivoCommand(VisuAlgParser.ArquivoCommandContext ctx) {
        TerminalNode text = ctx.STRING();
        return new ArquivoCommandNode(new StringLiteralNode(text.getText().substring(1, text.getText().length() - 1), fromTerminalNode(text)), fromRuleContext(ctx));
    }


    @Override
    public Node visitAleatorioCommand(VisuAlgParser.AleatorioCommandContext ctx) {
        if (ctx.OFF() != null) {
            return new AleatorioOffNode(fromRuleContext(ctx));
        }
        switch (ctx.expr().size()) {
            case 1 -> {
                return new AleatorioRangeNode(EmptyExpressionNode.INSTANCE, visitExpr(ctx.expr(0)), EmptyExpressionNode.INSTANCE, fromRuleContext(ctx));
            }
            case 2 -> {
                return new AleatorioRangeNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), EmptyExpressionNode.INSTANCE, fromRuleContext(ctx));
            }
            case 3 -> {
                return new AleatorioRangeNode(visitExpr(ctx.expr(0)), visitExpr(ctx.expr(1)), visitExpr(ctx.expr(2)), fromRuleContext(ctx));
            }
            default -> {}
        }
        return new AleatorioOnNode(fromRuleContext(ctx));
    }


    @Override
    public Node visitTimerCommand(VisuAlgParser.TimerCommandContext ctx) {
        boolean on = ctx.ON() != null;
        int value = Integer.parseInt(ctx.INT_LITERAL().getText());
        return new TimerCommandNode(on, value, fromRuleContext(ctx));
    }


    @Override
    public Node visitPausaCommand(VisuAlgParser.PausaCommandContext ctx) {
        return new PausaCommandNode(fromRuleContext(ctx));
    }


    @Override
    public Node visitDebugCommand(VisuAlgParser.DebugCommandContext ctx) {
        return new DebugCommandNode(visitExpr(ctx.expr()), fromRuleContext(ctx));
    }


    @Override
    public Node visitEcoCommand(VisuAlgParser.EcoCommandContext ctx) {
        boolean on = ctx.ON() != null;
        return new EcoCommandNode(on, fromRuleContext(ctx));
    }


    @Override
    public Node visitCronometroCommand(VisuAlgParser.CronometroCommandContext ctx) {
        boolean on = ctx.ON() != null;
        return new CronometroCommandNode(on, fromRuleContext(ctx));
    }


    @Override
    public Node visitLimpatelaCommand(VisuAlgParser.LimpatelaCommandContext ctx) {
        return new LimpatelaCommandNode(fromRuleContext(ctx));
    }

}

