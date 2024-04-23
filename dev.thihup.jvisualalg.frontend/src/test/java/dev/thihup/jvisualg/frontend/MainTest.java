package dev.thihup.jvisualg.frontend;

import dev.thihup.jvisualg.frontend.impl.antlr.VisuAlgParser;
import dev.thihup.jvisualg.frontend.node.Node;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

class MainTest {

    @ParameterizedTest
    @MethodSource("examplesV25")
    void testExamplesV25(Path path) throws Throwable {
        VisuAlgParser.AlgorithmContext parse = Main.parse(path);
        Node node = Main.buildAST(parse);
        System.out.println(Main.javaOutput(node));
    }

    @ParameterizedTest
    @MethodSource("examplesV30")
    void testExamplesV30(Path path) throws Throwable {
        VisuAlgParser.AlgorithmContext parse = Main.parse(path);
        Node node = Main.buildAST(parse);
        System.out.println(Main.javaOutput(node));
    }

    static Stream<Path> examplesV25() throws Throwable {
        return examples("v25");
    }

    static Stream<Path> examplesV30() throws Throwable {
        return examples("v30");
    }

    static Stream<Path> examples(String folder) throws Throwable {
        return Files.walk(Path.of("src", "test", "resources", "examples", folder))
                .filter(Files::isRegularFile);
    }

}
