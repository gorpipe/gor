package org.gorpipe.gor.reference;

import org.apache.commons.io.FileUtils;
import org.gorpipe.exceptions.GorSystemException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FolderMigrator {

    private FolderMigrator() {
    }

    public static void migrate(Path inputFolder, Path outputFolder) {
        Path tmpFolder = outputFolder.resolveSibling(outputFolder.getFileName() + "_tmp");
        if (Files.exists(tmpFolder)) {
            // Ignore, migration already on going or has fialed.
            return;
        }
        try {
            FileUtils.copyDirectory(inputFolder.toFile(), tmpFolder.toFile());
        } catch (IOException e) {
            throw new GorSystemException(
                    String.format("Could not copy (%s) folder into (%s)", inputFolder, outputFolder), e);
        }
        //copyDirectory(inputFolder, tmpFolder);
        try {
            Files.move(tmpFolder, outputFolder);
        } catch (IOException e) {
            throw new GorSystemException(
                    String.format("Could not move tmp (%s) folder into place (%s)", tmpFolder, outputFolder), e);
        }
    }

    // https://baptiste-wicht.com/posts/2010/08/file-copy-in-java-benchmark.html
    public static void copyDirectory(Path sourceFolder, Path destFolder)  {
        try {
            Files.walk(sourceFolder) //.collect(toList()).parallelStream()
                    .forEach(source -> {
                        Path destination = destFolder.resolve(source.relativize(sourceFolder));
                        if (Files.isDirectory(source)) {
                            try {
                                Files.createDirectories(destination);
                            } catch (IOException e) {
                                throw new GorSystemException(String.format("Problem creating directory %s", destination), e);
                            }
                        } else {
                            try {
                                Files.copy(source, destination);
                            } catch (IOException e) {
                                throw new GorSystemException(String.format("Problem copying %s -> %s", source, destination), e);
                            }
                        }
                    });
        } catch (IOException e) {
            throw new GorSystemException(String.format("Problem walking %s", sourceFolder), e);
        }
    }
}
