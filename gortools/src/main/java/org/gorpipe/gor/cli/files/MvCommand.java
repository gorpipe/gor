package org.gorpipe.gor.cli.files;

import org.gorpipe.gor.cli.HelpOptions;
import org.gorpipe.gor.model.FileReader;
import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(name = "mv", header = "Move or rename files.", description = "Move or rename files.")
public class MvCommand extends HelpOptions implements Runnable {

    @CommandLine.Parameters(index = "0", paramLabel = "SOURCE")
    private String source;

    @CommandLine.Parameters(index = "1", paramLabel = "DEST")
    private String dest;

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Explain what is being done.")
    private boolean verbose;

    @CommandLine.Option(names = {"-n", "--no-clobber"}, description = "Do not overwrite existing files.")
    private boolean noClobber;

    @CommandLine.Option(names = {"-f", "--force"}, description = "Overwrite destination, overriding -n.")
    private boolean force;

    @CommandLine.ParentCommand
    private FilesCommand parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        var reader = parent.getFileReader();
        try {
            if (shouldProceed(dest, reader)) {
                reader.move(source, dest);
                if (verbose) System.out.printf("'%s' -> '%s'%n", source, dest);
            }
        } catch (IOException e) {
            throw new CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
        }
    }

    private boolean shouldProceed(String destPath, FileReader reader) {
        if (!reader.exists(destPath)) return true;
        if (force) return true;
        if (noClobber) {
            if (verbose) System.err.printf("skipping '%s': destination exists%n", destPath);
            return false;
        }
        return true;
    }
}
