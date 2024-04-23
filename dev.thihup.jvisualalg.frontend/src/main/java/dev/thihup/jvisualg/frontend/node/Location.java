package dev.thihup.jvisualg.frontend.node;

import org.antlr.v4.runtime.ParserRuleContext;

public record Location(
    int startLine,
    int startColumn,
    int endLine,
    int endColumn
) {
    public Location {
    }

    public static Location fromRuleContext(ParserRuleContext ctx) {
        return new Location(
            ctx.start.getLine(),
            ctx.start.getCharPositionInLine(),
            ctx.stop.getLine(),
            ctx.stop.getCharPositionInLine()
        );
    }

}
