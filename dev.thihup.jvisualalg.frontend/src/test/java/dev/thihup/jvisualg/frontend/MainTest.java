package dev.thihup.jvisualg.frontend;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

class MainTest {

    @ParameterizedTest
    @MethodSource("examples")
    void testMain(Path path) throws Throwable {
        Main.parse(path);
    }

    static Stream<Path> examples() throws Throwable {
        return Files.walk(Path.of("src", "test", "resources", "examples"))
                .filter(Files::isRegularFile);
    }

}
