package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgParser;
import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgParserBaseVisitor;
import dev.thihup.jvisualg.frontend.node.Location;
import dev.thihup.jvisualg.frontend.node.Node;
import dev.thihup.jvisualg.frontend.node.Node.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class VisuAlgParserVisitor extends VisuAlgParserBaseVisitor<Node> {


    @Override
    public Node visitAlgorithm(VisuAlgParser.AlgorithmContext ctx) {
        return new AlgoritimoNode(ctx.STRING().getText(), visit(ctx.declarations()), visit(ctx.commands()), Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitDeclarations(VisuAlgParser.DeclarationsContext ctx) {
        return new CompundNode(ctx.children.stream().map(this::visit).filter(Objects::nonNull).toList(), Location.fromRuleContext(ctx));
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
    public Node visitFormalParameter(VisuAlgParser.FormalParameterContext ctx) {
        return new CompundNode(ctx.ID().stream().map(TerminalNode::getText).map(x -> new VariableDeclarationNode(x, visit(ctx.type()), Location.fromRuleContext(ctx))).toList(), null);
    }

    @Override
    public Node visitProcedureDeclaration(VisuAlgParser.ProcedureDeclarationContext ctx) {
        List<Node> parameters = ctx.formalParameterList() instanceof VisuAlgParser.FormalParameterListContext context ? context.formalParameter().stream().map(this::visit).toList() : List.of();
        List<Node> declarations = ctx.declarations() instanceof VisuAlgParser.DeclarationsContext context ? context.variableDeclaration().stream().map(this::visit).toList() : List.of();
        List<Node> commands = ctx.commands() instanceof VisuAlgParser.CommandsContext context ? context.command().stream().map(this::visit).toList() : List.of();
        return new ProcedureDeclarationNode(
            ctx.ID().getText(),
                parameters,
                List.of(),
                declarations,
                commands,
            Location.fromRuleContext(ctx)
        );
    }

    @Override
    public Node visitFunctionDeclaration(VisuAlgParser.FunctionDeclarationContext ctx) {
        List<Node> parameters = ctx.formalParameterList() instanceof VisuAlgParser.FormalParameterListContext context ? context.formalParameter().stream().map(this::visit).toList() : List.of();
        List<Node> declarations = ctx.declarations() instanceof VisuAlgParser.DeclarationsContext context ? context.variableDeclaration().stream().map(this::visit).toList() : List.of();
        List<Node> commands = ctx.commands() instanceof VisuAlgParser.CommandsContext context ? context.command().stream().map(this::visit).toList() : List.of();
        return new FunctionDeclarationNode(
                ctx.ID().getText(),
                visitType(ctx.type()),
                parameters,
                List.of(),
                declarations,
                commands,
                Location.fromRuleContext(ctx)
        );
    }

    @Override
    public Node visitRegistroDeclaration(VisuAlgParser.RegistroDeclarationContext ctx) {
        List<Node> variableDeclarationContexts = ctx.variableDeclaration().stream().map(this::visit).toList();
        return new RegistroDeclarationNode(ctx.ID().getText(), variableDeclarationContexts, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitVariableDeclaration(VisuAlgParser.VariableDeclarationContext ctx) {
        List<VariableDeclarationNode> variables = ctx.ID().stream().map(TerminalNode::getText)
                .map(x -> new VariableDeclarationNode(x, visit(ctx.type()), Location.fromRuleContext(ctx)))
                .toList();
        return new CompundNode(variables, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitConstantsDeclaration(VisuAlgParser.ConstantsDeclarationContext ctx) {
        List<ConstantNode> constants = new ArrayList<>();
        for (int i = 0; i < ctx.ID().size(); i++) {
            constants.add(new ConstantNode(ctx.ID(i).getText(), visit(ctx.expr(i)), Location.fromRuleContext(ctx)));
        }
        return new ConstantsDeclarationNode(constants, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitLiterais(VisuAlgParser.LiteraisContext ctx) {
        TerminalNode falso = ctx.FALSO();
        if (falso != null) {
            return new BooleanLiteralNode(false, Location.fromRuleContext(ctx));
        }
        TerminalNode verdadeiro = ctx.VERDADEIRO();
        if (verdadeiro != null) {
            return new BooleanLiteralNode(true, Location.fromRuleContext(ctx));
        }
        TerminalNode string = ctx.STRING();
        if (string != null) {
            return new StringLiteralNode(string.getText(), Location.fromRuleContext(ctx));
        }
        TerminalNode real = ctx.REAL_LITERAL();
        if (real != null) {
            return new RealLiteralNode(Double.parseDouble(real.getText()), Location.fromRuleContext(ctx));
        }
        TerminalNode inteiro = ctx.INT_LITERAL();
        if (inteiro != null) {
            return new IntLiteralNode(Integer.parseInt(inteiro.getText()), Location.fromRuleContext(ctx));
        }
        throw new RuntimeException("Unknown literal: " + ctx.getText());
    }


    @Override
    public Node visitType(VisuAlgParser.TypeContext ctx) {
        TerminalNode vetor = ctx.VETOR();
        if (vetor == null)
            return new TypeNode(ctx.getText(), Location.fromRuleContext(ctx));
        List<TerminalNode> range = ctx.RANGE();


        List<Node> list = range.stream()
                .map(this::rangeToNode)
                .toList();
        return new ArrayTypeNode((TypeNode) visit(ctx.type()), range.size(), list, Location.fromRuleContext(ctx));
    }

    private Node rangeToNode(TerminalNode ctx) {
        // range is (number)..(number)  or (number)..(text) or (text)..(number) or (text)..(text)
        // and the returned node should be a SubNode from the second number to the first number
        String[] values = ctx.getText().split("\\.\\.");
        assert values.length == 2;
        Integer min, max;
        try {
            min = Integer.parseInt(values[0]);
        } catch (NumberFormatException e) {
            min = null;
        }
        try {
            max = Integer.parseInt(values[1]);
        } catch (NumberFormatException e) {
            max = null;
        }

        return new AddNode(new SubNode(max != null ? new IntLiteralNode(max, null) : new IdNode(values[1], null),
                min != null ? new IntLiteralNode(min, null) : new IdNode(values[0], null), null), new IntLiteralNode(1, null), null);
    }

    @Override
    public Node visitCommands(VisuAlgParser.CommandsContext ctx) {
        List<Node> commands = ctx.command().stream().map(this::visit).toList();
        return new CommandsNode(commands, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitCommand(VisuAlgParser.CommandContext ctx) {
        return new CommandNode(visit(ctx.getChild(0)), Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitInterrompaCommand(VisuAlgParser.InterrompaCommandContext ctx) {
        return new InterrompaCommandNode(Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitReturn(VisuAlgParser.ReturnContext ctx) {
        return new ReturnNode(visit(ctx.expr()), Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitDos(VisuAlgParser.DosContext ctx) {
        return new DosNode(Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitAssignment(VisuAlgParser.AssignmentContext ctx) {
        return new AssignmentNode(visit(ctx.idOrArray()), visit(ctx.expr()), Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitIdOrArray(VisuAlgParser.IdOrArrayContext ctx) {
        return ctx.children
            .stream()
            .map(this::visit)
            .reduce((result, element) -> switch (element) {
                case ArrayAccessNode(_, List<Node> indexes, _) ->
                        new ArrayAccessNode(result, indexes, Location.fromRuleContext(ctx));
                case MemberAccessNode(_, Node expr, Location _) ->
                        new MemberAccessNode(result, expr, Location.fromRuleContext(ctx));
                case IdNode id -> new IdNode(id.id(), Location.fromRuleContext(ctx));
                default -> throw new IllegalStateException();
            })
        .orElseThrow();
    }

    @Override
    public Node visitMemberAccess(VisuAlgParser.MemberAccessContext ctx) {
        return new MemberAccessNode(null, visit(ctx.idOrArray()), Location.fromRuleContext(ctx));
    }

    @Override
    public Node visitArrayAccess(VisuAlgParser.ArrayAccessContext ctx) {
        return new ArrayAccessNode(null, ctx.expr().stream().map(this::visit).toList(), Location.fromRuleContext(ctx));
    }

    @Override
    public IdNode visitId(VisuAlgParser.IdContext ctx) {
        return new IdNode(ctx.ID().getText(), Location.fromRuleContext(ctx));
    }

    @Override
    public Node visitReadCommand(VisuAlgParser.ReadCommandContext ctx) {
        List<Node> leia = ctx.exprList()
                .expr()
                .stream()
                .map(this::visit)
                .toList();

        return new CommandNode(new ReadCommandNode(leia, Location.fromRuleContext(ctx)), Location.fromRuleContext(ctx));
    }




    @Override
    public Node visitWriteCommand(VisuAlgParser.WriteCommandContext ctx) {
        boolean newLine = ctx.ESCREVAL() != null;
        VisuAlgParser.WriteListContext writeListContext = ctx.writeList();
        if (writeListContext == null) {
            return new WriteCommandNode(newLine, List.of(), Location.fromRuleContext(ctx));
        }
        List<Node> writeList = writeListContext.writeItem().stream().map(this::visit).toList();
        return new CommandNode(new WriteCommandNode(newLine, writeList, Location.fromRuleContext(ctx)), Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitWriteItem(VisuAlgParser.WriteItemContext ctx) {
        Node expr = visit(ctx.expr());
        TerminalNode spaces = ctx.INT_LITERAL(0);
        TerminalNode precision = ctx.INT_LITERAL(1);
        Integer spaces1 = spaces != null ? Integer.parseInt(spaces.getText()) : null;
        Integer precision1 = precision != null ? Integer.parseInt(precision.getText()) : null;
        return new CommandNode(new WriteItemNode(expr, spaces1, precision1, Location.fromRuleContext(ctx)), Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitConditionalCommand(VisuAlgParser.ConditionalCommandContext ctx) {
        Node expr = visit(ctx.expr());
        List<Node> commands = ctx.commands(0).command().stream().map(this::visit).toList();
        VisuAlgParser.CommandsContext commands1 = ctx.commands(1);
        if (commands1 == null || commands1.command().isEmpty()) {
            return new ConditionalCommandNode(expr, commands, List.of(), Location.fromRuleContext(ctx));
        }
        List<Node> elseCommands = commands1.command().stream().map(this::visit).toList();
        return new ConditionalCommandNode(expr, commands, elseCommands, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitChooseCommand(VisuAlgParser.ChooseCommandContext ctx) {
        Node expr = visit(ctx.expr());
        List<Node> cases = ctx.chooseCase().stream().map(this::visit).toList();
        VisuAlgParser.OutroCaseContext outroCaseContext = ctx.outroCase();
        Node defaultCase = outroCaseContext != null ? visit(outroCaseContext) : null;
        return new ChooseCommandNode(expr, cases, defaultCase, Location.fromRuleContext(ctx));
    }

    @Override
    public Node visitChooseCase(VisuAlgParser.ChooseCaseContext ctx) {
        Node expr = visit(ctx.exprOrAteList());
        List<Node> commands = ctx.commands().command().stream().map(this::visit).toList();
        return new ChooseCaseNode(expr, commands, Location.fromRuleContext(ctx));
    }

    @Override
    public Node visitOutroCase(VisuAlgParser.OutroCaseContext ctx) {
        List<Node> commands = ctx.commands().command().stream().map(this::visit).toList();
        return new ChooseCaseNode(null, commands, Location.fromRuleContext(ctx));
    }

    @Override
    public Node visitExprOrAte(VisuAlgParser.ExprOrAteContext ctx) {
        if (ctx.ATE() != null) {
            return new RangeNode(visit(ctx.expr(0)), visit(ctx.expr(1)),Location.fromRuleContext(ctx));
        }
        return visit(ctx.expr(0));
    }

    @Override
    public Node visitParaCommand(VisuAlgParser.ParaCommandContext ctx) {
        IdNode idOrArray = new IdNode(ctx.ID().getText(), Location.fromRuleContext(ctx));
        Node endValue = ctx.expr(1) instanceof VisuAlgParser.ExprContext endValueExpr ? visit(endValueExpr) : null;
        Node step = ctx.expr(2) instanceof VisuAlgParser.ExprContext stepValue ? visit(stepValue) : new IntLiteralNode(1, null);
        List<Node> commands = ctx.commands().command().stream().map(this::visit).toList();


        return new ForCommandNode(
            idOrArray,
            visit(ctx.expr(0)),
            endValue,
            step,
            commands,
            Location.fromRuleContext(ctx));
    }

    @Override
    public Node visitExpr(VisuAlgParser.ExprContext ctx) {
        if (ctx.atom() instanceof VisuAlgParser.AtomContext atomContext) {
            return visit(atomContext);
        }
        if (ctx.parenExpression() instanceof VisuAlgParser.ParenExpressionContext parenExpressionContext) {
            return visit(parenExpressionContext.expr());
        }
        if (ctx.SUB() != null) {
            return new NegNode(visit(ctx.expr(0)), Location.fromRuleContext(ctx));
        }
        if (ctx.NOT() != null) {
            return new NotNode(visit(ctx.expr(0)), Location.fromRuleContext(ctx));
        }
        String text = ctx.getChild(1).getText();
        return switch (text.toLowerCase()) {
            case "+" -> new AddNode(visit(ctx.expr(0)), visit(ctx.expr(1)), Location.fromRuleContext(ctx));
            case "-" -> new SubNode(visit(ctx.expr(0)), visit(ctx.expr(1)), Location.fromRuleContext(ctx));
            case "*" -> new MulNode(visit(ctx.expr(0)), visit(ctx.expr(1)), Location.fromRuleContext(ctx));
            case "/", "div", "\\" -> new DivNode(visit(ctx.expr(0)), visit(ctx.expr(1)), Location.fromRuleContext(ctx));
            case "e" -> new AndNode(visit(ctx.expr(0)), visit(ctx.expr(1)), Location.fromRuleContext(ctx));
            case "ou" -> new OrNode(visit(ctx.expr(0)), visit(ctx.expr(1)), Location.fromRuleContext(ctx));
            case "nao", "não" -> new NotNode(visit(ctx.expr(0)), Location.fromRuleContext(ctx));
            case "=" -> new EqNode(visit(ctx.expr(0)), visit(ctx.expr(1)), Location.fromRuleContext(ctx));
            case "<" -> new LtNode(visit(ctx.expr(0)), visit(ctx.expr(1)), Location.fromRuleContext(ctx));
            case ">" -> new GtNode(visit(ctx.expr(0)), visit(ctx.expr(1)), Location.fromRuleContext(ctx));
            case "<=" -> new LeNode(visit(ctx.expr(0)), visit(ctx.expr(1)), Location.fromRuleContext(ctx));
            case ">=" -> new GeNode(visit(ctx.expr(0)), visit(ctx.expr(1)), Location.fromRuleContext(ctx));
            case "<>" -> new NeNode(visit(ctx.expr(0)), visit(ctx.expr(1)), Location.fromRuleContext(ctx));
            case "%", "mod" -> new ModNode(visit(ctx.expr(0)), visit(ctx.expr(1)), Location.fromRuleContext(ctx));
            case "pot" -> new PowNode(visit(ctx.expr(0)), visit(ctx.expr(1)), Location.fromRuleContext(ctx));
            default -> throw new UnsupportedOperationException(text + " -> " + ctx.getParent().getText());
        };
    }

    @Override
    public Node visitAtom(VisuAlgParser.AtomContext ctx) {
        if (ctx.idOrArray() != null) {
            return visit(ctx.idOrArray());
        }
        if (ctx.literais() != null) {
            return visit(ctx.literais());
        }
        if (ctx.functionCall() != null) {
            return visit(ctx.functionCall());
        }
        throw new UnsupportedOperationException(ctx.getText());
    }



    @Override
    public Node visitEnquantoCommand(VisuAlgParser.EnquantoCommandContext ctx) {
        Node test = visit(ctx.expr());
        List<Node> commands = ctx.commands().command().stream().map(this::visit).toList();
        return new WhileCommandNode(test, commands, false, Location.fromRuleContext(ctx));
    }

    @Override
    public Node visitRepitaCommand(VisuAlgParser.RepitaCommandContext ctx) {
        Node test = ctx.expr() instanceof VisuAlgParser.ExprContext v ? visit(v) : new BooleanLiteralNode(true, Location.fromRuleContext(ctx));
        List<Node> commands = ctx.commands().command().stream().map(this::visit).toList();
        return new WhileCommandNode(test, commands, true, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitProcedureCall(VisuAlgParser.ProcedureCallContext ctx) {
        String name = ctx.ID().getText();
        VisuAlgParser.ExprListContext exprListContext = ctx.exprList();
        if (exprListContext == null) {
            return new ProcedureCallNode(name, List.of(), Location.fromRuleContext(ctx));
        }
        List<Node> args = exprListContext.expr().stream().map(this::visit).toList();
        return new ProcedureCallNode(name, args, Location.fromRuleContext(ctx));
    }

    @Override
    public Node visitFunctionCall(VisuAlgParser.FunctionCallContext ctx) {
        String name = ctx.ID().getText();
        VisuAlgParser.ExprListContext exprListContext = ctx.exprList();
        if (exprListContext == null) {
            return new FunctionCallNode(name, List.of(), Location.fromRuleContext(ctx));
        }
        List<Node> args = exprListContext.expr().stream().map(this::visit).toList();
        return new FunctionCallNode(name, args, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitArquivoCommand(VisuAlgParser.ArquivoCommandContext ctx) {
        return new ArquivoCommandNode(ctx.STRING().getText(), Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitAleatorioCommand(VisuAlgParser.AleatorioCommandContext ctx) {
        boolean on = ctx.ON() != null;
        List<Integer> args = ctx.INT_LITERAL().stream().map(TerminalNode::getText).map(Integer::parseInt).toList();
        return new AleatorioCommandNode(on, args, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitTimerCommand(VisuAlgParser.TimerCommandContext ctx) {
        boolean on = ctx.ON() != null;
        int value = Integer.parseInt(ctx.INT_LITERAL().getText());
        return new TimerCommandNode(on, value, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitPausaCommand(VisuAlgParser.PausaCommandContext ctx) {
        return new PausaCommandNode(Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitDebugCommand(VisuAlgParser.DebugCommandContext ctx) {
        return new DebugCommandNode(visit(ctx.expr()), Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitEcoCommand(VisuAlgParser.EcoCommandContext ctx) {
        boolean on = ctx.ON() != null;
        return new EcoCommandNode(on, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitCronometroCommand(VisuAlgParser.CronometroCommandContext ctx) {
        boolean on = ctx.ON() != null;
        return new CronometroCommandNode(on, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitLimpatelaCommand(VisuAlgParser.LimpatelaCommandContext ctx) {
        return new LimpatelaCommandNode(Location.fromRuleContext(ctx));
    }

}

