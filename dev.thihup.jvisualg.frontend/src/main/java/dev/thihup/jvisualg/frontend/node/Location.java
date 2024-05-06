package dev.thihup.jvisualg.frontend.node;

import org.antlr.v4.runtime.ParserRuleContext;

public record Location(
    int startLine,
    int startColumn,
    int endLine,
    int endColumn
) {

    public static final Location EMPTY = new Location(0, 0, 0, 0);

    public Location {
    }

    public String toString() {
        return startLine + ":" + startColumn + "-" + endLine + ":" + endColumn;
    }

    public boolean isInside(Location other) {
        return startLine >= other.startLine && endLine <= other.endLine &&
            startColumn >= other.startColumn && endColumn <= other.endColumn;
    }

}
