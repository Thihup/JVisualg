package dev.thihup.jvisualg.ide;

import dev.thihup.jvisualg.frontend.ASTResult;
import dev.thihup.jvisualg.frontend.Error;
import dev.thihup.jvisualg.frontend.TypeCheckerResult;
import dev.thihup.jvisualg.frontend.VisualgParser;
import org.fife.ui.rsyntaxtextarea.RSyntaxDocument;
import org.fife.ui.rsyntaxtextarea.parser.AbstractParser;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParseResult;
import org.fife.ui.rsyntaxtextarea.parser.DefaultParserNotice;
import org.fife.ui.rsyntaxtextarea.parser.ParseResult;

import java.util.List;

class TypeChecker extends AbstractParser {
    @Override
    public ParseResult parse(RSyntaxDocument doc, String style) {
        DefaultParseResult parseResult = new DefaultParseResult(this);
        long start = System.currentTimeMillis();
        parseResult.setParsedLines(0, doc.getDefaultRootElement().getElementCount());

        try {
            String text = doc.getText(0, doc.getLength());
            ASTResult astResult = VisualgParser.parse(text);

            List<Error> errors = astResult.errors();

            astResult.node()
                    .map(dev.thihup.jvisualg.frontend.TypeChecker::semanticAnalysis)
                    .map(TypeCheckerResult::errors)
                    .ifPresent(errors::addAll);

            errors.stream()
                    .map(x -> {
                        int offset = doc.getTokenListForLine(x.location().startLine() - 1).getOffset();
                        return new DefaultParserNotice(this, x.message(), x.location().startLine() - 1, offset + x.location().startColumn(), x.location().endColumn() - x.location().startColumn() + 1);
                    })
                    .forEach(parseResult::addNotice);
        } catch (Exception e) {
            parseResult.setError(e);
        }
        parseResult.setParseTime(System.currentTimeMillis() - start);
        return parseResult;
    }
}
