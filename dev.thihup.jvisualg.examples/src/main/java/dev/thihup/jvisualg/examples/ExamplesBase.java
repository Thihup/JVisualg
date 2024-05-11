package dev.thihup.jvisualg.examples;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public class ExamplesBase {

    static Stream<Path> examplesV25() throws Throwable {
        return examples("v25");
    }

    static Stream<Path> examplesV30() throws Throwable {
        return examples("v30");
    }

    public static Stream<Path> examples(String folder) throws Throwable {
        return Files.walk(Path.of(ExamplesBase.class.getResource(folder).toURI()))
                .filter(Files::isRegularFile);
    }

}
