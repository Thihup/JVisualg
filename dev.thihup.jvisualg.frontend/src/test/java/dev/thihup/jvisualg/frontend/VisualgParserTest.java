package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.examples.ExamplesBase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;

class VisualgParserTest extends ExamplesBase {

    @ParameterizedTest
    @MethodSource("examplesV25")
    void testExamplesV25(Path path) throws Throwable {
        ASTResult astResult = VisualgParser.parse(Files.newInputStream(path));
        TypeCheckerResult typecheckResult = TypeChecker.semanticAnalysis(astResult.node().get());
        typecheckResult.errors().forEach(x -> System.out.println(path.toString().replace('\\', '/') + ":" + x));
    }

    @ParameterizedTest
    @MethodSource("examplesV30")
    void testExamplesV30(Path path) throws Throwable {
        ASTResult astResult = VisualgParser.parse(Files.newInputStream(path));
        TypeCheckerResult typecheckResult = TypeChecker.semanticAnalysis(astResult.node().get());
        typecheckResult.errors().forEach(x -> System.out.println(path.toString().replace('\\', '/') + ":" + x));
    }



}
