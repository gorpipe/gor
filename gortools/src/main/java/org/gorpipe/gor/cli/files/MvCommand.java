package org.gorpipe.gor.cli.files;

import java.io.IOException;

import picocli.CommandLine;

@CommandLine.Command(name = "mv", description = "Move or rename files.")
public class MvCommand implements Runnable {

    @CommandLine.Parameters(index = "0", paramLabel = "SOURCE")
    private String source;

    @CommandLine.Parameters(index = "1", paramLabel = "DEST")
    private String dest;

    @CommandLine.ParentCommand
    private FilesCommand parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        var reader = parent.getFileReader();
        try {
            reader.move(source, dest);
        } catch (IOException e) {
            throw new CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
        }
    }
}
