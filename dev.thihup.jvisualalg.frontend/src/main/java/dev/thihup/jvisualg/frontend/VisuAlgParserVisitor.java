package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgParser;
import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgParserBaseVisitor;
import dev.thihup.jvisualg.frontend.node.Location;
import dev.thihup.jvisualg.frontend.node.Node;
import dev.thihup.jvisualg.frontend.node.Node.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import java.util.List;

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
        return new TypeNode(ctx.getText(), Location.fromRuleContext(ctx));
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
        List<Node> indexes = ctx.expr().stream().map(this::visit).toList();
        return new IdOrArrayNode("????", indexes, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitReadCommand(VisuAlgParser.ReadCommandContext ctx) {
        List<Node> exprList = ctx.exprList().expr().stream().map(this::visit).toList();
        return new ReadCommandNode(exprList, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitWriteCommand(VisuAlgParser.WriteCommandContext ctx) {
        boolean newLine = ctx.ESCREVAL() != null;
        List<Node> writeList = ctx.writeList().writeItem().stream().map(this::visit).toList();
        return new WriteCommandNode(newLine, writeList, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitWriteItem(VisuAlgParser.WriteItemContext ctx) {
        Node expr = visit(ctx.expr());
        String format = ctx.INT_LITERAL().stream().map(TerminalNode::getText).reduce("", (a, b) -> a + b);
        return new WriteItemNode(expr, format, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitConditionalCommand(VisuAlgParser.ConditionalCommandContext ctx) {
        Node expr = visit(ctx.expr().getFirst());
        List<Node> commands = ctx.commands(0).command().stream().map(this::visit).toList();
        List<Node> elseCommands = ctx.commands(1).command().stream().map(this::visit).toList();
        return new ConditionalCommandNode(expr, commands, elseCommands, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitChooseCommand(VisuAlgParser.ChooseCommandContext ctx) {
        throw new UnsupportedOperationException();
    }


    @Override
    public Node visitParaCommand(VisuAlgParser.ParaCommandContext ctx) {
        Node id = visit(ctx.ID());
        Node start = visit(ctx.expr(0));
        Node end = visit(ctx.expr(1));
        Node step = visit(ctx.expr(2));
        List<Node> commands = ctx.commands().command().stream().map(this::visit).toList();
        return new LoopCommandNode(id, start, end, step, commands, Location.fromRuleContext(ctx));
    }

    @Override
    public Node visitEnquantoCommand(VisuAlgParser.EnquantoCommandContext ctx) {
        Node expr = visit(ctx.expr());
        List<Node> commands = ctx.commands().command().stream().map(this::visit).toList();
        return new LoopCommandNode(null, null, null, null, commands, Location.fromRuleContext(ctx));
    }

    @Override
    public Node visitRepitaCommand(VisuAlgParser.RepitaCommandContext ctx) {
        Node expr = visit(ctx.expr());
        List<Node> commands = ctx.commands().command().stream().map(this::visit).toList();
        return new LoopCommandNode(null, null, null, null, commands, Location.fromRuleContext(ctx));
    }


    @Override
    public Node visitSubprogramCall(VisuAlgParser.SubprogramCallContext ctx) {
        String name = ctx.ID().getText();
        List<Node> args = ctx.exprList().expr().stream().map(this::visit).toList();
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

