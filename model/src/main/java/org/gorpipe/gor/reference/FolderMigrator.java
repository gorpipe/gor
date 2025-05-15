package org.gorpipe.gor.reference;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.gorpipe.exceptions.GorSystemException;

import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;

public class FolderMigrator {

    private FolderMigrator() {
    }

    public static void migrate(Path inputFolder, Path outputFolder) throws IOException{
        if (inputFolder == null || outputFolder == null) {
            // Nothing to do.
            return;
        }

        String tempFileRoot = outputFolder.getFileName() + "_tmp_";

        if (Files.exists(outputFolder.getParent())
                && Files.list(outputFolder.getParent()).anyMatch(p -> p.getFileName().toString().startsWith(tempFileRoot))) {
            // Ignore, migration already on going, or has failed.
            return;
        }

        Path tmpFolder = outputFolder.resolveSibling(tempFileRoot + RandomStringUtils.insecure().next(10, true, true));
        tmpFolder.toFile().deleteOnExit();

        FileUtils.copyDirectory(inputFolder.toFile(), tmpFolder.toFile(), (FileFilter)null, true,
                NOFOLLOW_LINKS);
        //copyDirectory(inputFolder, tmpFolder);

        Files.move(tmpFolder, outputFolder);
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
