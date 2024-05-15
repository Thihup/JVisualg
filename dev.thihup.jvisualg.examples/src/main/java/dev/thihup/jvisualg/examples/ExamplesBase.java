package dev.thihup.jvisualg.examples;

import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

public class ExamplesBase {

    static Stream<Path> examplesV25() throws Throwable {
        return examples("v25");
    }

    static Stream<Path> examplesV30() throws Throwable {
        return examples("v30");
    }

    public static Stream<Path> examples(String folder) throws Throwable {
        URI start = ExamplesBase.class.getResource(folder).toURI();
        try {
            FileSystem fileSystem = FileSystems.newFileSystem(start, Map.of());
            return Files.walk(fileSystem.getPath(ExamplesBase.class.getPackageName().replace('.', '/') + "/" + folder))
                .filter(Files::isRegularFile);
        } catch (Exception e) {
            return Files.walk(Path.of(start))
                    .filter(Files::isRegularFile);
        }
    }

}
