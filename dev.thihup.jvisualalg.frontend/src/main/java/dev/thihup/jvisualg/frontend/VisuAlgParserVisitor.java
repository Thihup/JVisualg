package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgParser;
import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgParserBaseVisitor;
import dev.thihup.jvisualg.frontend.node.Location;
import dev.thihup.jvisualg.frontend.node.Node;
import dev.thihup.jvisualg.frontend.node.Node.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;
import java.util.stream.IntStream;

public class VisuAlgParserVisitor extends VisuAlgParserBaseVisitor<Node> {


    @Override
    public Node visitAlgorithm(VisuAlgParser.AlgorithmContext ctx) {
        return new AlgoritimoNode(ctx.STRING().getText(), visit(ctx.declarations()), visit(ctx.commands()), Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitDeclarations(VisuAlgParser.DeclarationsContext ctx) {
        List<Node> variableDeclarationContexts = ctx.variableDeclaration().stream().map(this::visit).toList();
        List<Node> registroDeclarationContexts = ctx.registroDeclaration().stream().map(this::visit).toList();
        List<Node> subprogramDeclarationContexts = ctx.subprogramDeclaration().stream().map(this::visit).toList();
        List<Node> constantsDeclarationContexts = ctx.constantsDeclaration().stream().map(this::visit).toList();
        List<Node> dosContexts = ctx.dos().stream().map(this::visit).toList();
        return new DeclarationsNode(variableDeclarationContexts, registroDeclarationContexts, subprogramDeclarationContexts, constantsDeclarationContexts, dosContexts, Location.fromRuleContext(ctx));
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
        List<ConstantNode> constants = ctx.ID().stream().map(TerminalNode::getText)
                .map(x -> new ConstantNode(x, visit(ctx.literais(0)), Location.fromRuleContext(ctx)))
                .toList();
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
        List<TerminalNode> id = ctx.ID();
        List<VisuAlgParser.ExprContext> expr = ctx.expr();
        IdNode idNode = new IdNode(id.getFirst().getText(), Location.fromRuleContext(ctx));
        if (expr == null || expr.isEmpty()) {
            return idNode;
        }
        return new ArrayAccessNode(idNode, expr.stream().map(this::visit).toList(), Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitReadCommand(VisuAlgParser.ReadCommandContext ctx) {
        List<AssignmentNode> leia = ctx.exprList()
                .expr()
                .stream()
                .map(this::visit)
                .map(x -> new AssignmentNode(x, new SubprogramCallNode("leia", List.of(), Location.fromRuleContext(ctx)), Location.fromRuleContext(ctx)))
                .toList();

        if (leia.size() == 1)
            return leia.getFirst();
        return new CompundNode(leia, Location.fromRuleContext(ctx));
    }




    @Override
    public Node visitWriteCommand(VisuAlgParser.WriteCommandContext ctx) {
        boolean newLine = ctx.ESCREVAL() != null;
        VisuAlgParser.WriteListContext writeListContext = ctx.writeList();
        if (writeListContext == null) {
            return new WriteCommandNode(newLine, List.of(), Location.fromRuleContext(ctx));
        }
        List<Node> writeList = writeListContext.writeItem().stream().map(this::visit).toList();
        return new WriteCommandNode(newLine, writeList, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitWriteItem(VisuAlgParser.WriteItemContext ctx) {
        Node expr = visit(ctx.expr());
        TerminalNode spaces = ctx.INT_LITERAL(0);
        TerminalNode precision = ctx.INT_LITERAL(1);
        Integer spaces1 = spaces != null ? Integer.parseInt(spaces.getText()) : null;
        Integer precision1 = precision != null ? Integer.parseInt(precision.getText()) : null;
        return new WriteItemNode(expr, spaces1, precision1, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitConditionalCommand(VisuAlgParser.ConditionalCommandContext ctx) {
        Node expr = visit(ctx.expr().getFirst());
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
        Node expr = exprsToNode(ctx.expr());

        List<Node> list = ctx.exprOrAte().stream().map(this::visit).toList();
        TerminalNode outrocaso = ctx.OUTROCASO();
        if (outrocaso == null) {
            return new ChooseCommandNode(expr, list, null, Location.fromRuleContext(ctx));
        }
        return new ChooseCommandNode(expr, list, visit(outrocaso), Location.fromRuleContext(ctx));
    }

    Node exprsToNode(List<VisuAlgParser.ExprContext> exprs) {
        if (exprs.size() == 1) {
            return visit(exprs.getFirst());
        }
        return new CompundNode(exprs.stream().map(this::visit).toList(), Location.fromRuleContext(exprs.getFirst()));
    }


    @Override
    public Node visitParaCommand(VisuAlgParser.ParaCommandContext ctx) {
        Node visit = visit(ctx.expr(0));
        IdNode idOrArray = new IdNode(ctx.ID().getText(), Location.fromRuleContext(ctx));
        Node test = ctx.expr(1) instanceof VisuAlgParser.ExprContext testConditional ? new LeNode(idOrArray, visit(testConditional), null) : new BooleanLiteralNode(false, Location.fromRuleContext(ctx));
        Node step = ctx.expr(2) instanceof VisuAlgParser.ExprContext stepValue ? new IncrementNode(idOrArray, visit(stepValue), null) : new IncrementNode(idOrArray, new IntLiteralNode(1, null), null);
        List<Node> commands = ctx.commands().command().stream().map(this::visit).toList();


        return new ForCommandNode(
            new AssignmentNode(idOrArray, visit, Location.fromRuleContext(ctx)),
            test,
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
            return new IdNode(ctx.idOrArray().getText(), Location.fromRuleContext(ctx));
        }
        if (ctx.literais() != null) {
            return visit(ctx.literais());
        }
        if (ctx.subprogramCall() != null) {
            return visit(ctx.subprogramCall());
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
    public Node visitSubprogramCall(VisuAlgParser.SubprogramCallContext ctx) {
        String name = ctx.ID().getText();
        VisuAlgParser.ExprListContext exprListContext = ctx.exprList();
        if (exprListContext == null) {
            return new SubprogramCallNode(name, List.of(), Location.fromRuleContext(ctx));
        }
        List<Node> args = exprListContext.expr().stream().map(this::visit).toList();
        return new SubprogramCallNode(name, args, Location.fromRuleContext(ctx));
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

