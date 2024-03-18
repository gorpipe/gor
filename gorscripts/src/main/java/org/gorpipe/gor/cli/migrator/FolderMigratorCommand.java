package org.gorpipe.gor.cli.migrator;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.cli.HelpOptions;
import org.gorpipe.gor.reference.FolderMigrator;
import picocli.CommandLine;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@CommandLine.Command(name = "migrate",
        aliases = {},
        description="Migrate data folders",
        header="Migrate data folders")
public class FolderMigratorCommand extends HelpOptions implements Runnable {

    @CommandLine.Parameters(
            index = "0",
            arity = "1",
            paramLabel = "InputFolder",
            description = "Folder to migrate/copy from.")
    private String inputFolder;

    @CommandLine.Parameters(
            index = "1",
            arity = "1",
            paramLabel = "OutputFolder",
            description = "Folder to migrate/copy to.")
    private String outputFolder;

    @Override
    public void run() {
        try {
            Path input = Paths.get(inputFolder);
            if (!Files.exists(input)) {
                throw new GorResourceException("Input folder does not exist", inputFolder);
            }
            if (!Files.isDirectory(input)) {
                throw new GorResourceException("Input folder is not a directory", inputFolder);
            }

            if (outputFolder == null || outputFolder.isEmpty()) {
                throw new GorResourceException("No output folder specified", outputFolder);
            }

            FolderMigrator.migrate(input, Paths.get(outputFolder));
        } catch (Exception e) {
            throw new GorSystemException(e);
        }
    }
}
