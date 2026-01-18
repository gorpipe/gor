package org.gorpipe.gor.cli.files;

import java.io.IOException;

import picocli.CommandLine;

@CommandLine.Command(name = "cat", description = "Concatenate files to standard output.")
public class CatCommand implements Runnable {

    @CommandLine.Parameters(index = "0", paramLabel = "PATH")
    private String path;

    @CommandLine.ParentCommand
    private FilesCommand parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        var reader = parent.getFileReader();
        try (var stream = reader.readFile(path)) {
            stream.forEach(parent.getStdOut()::println);
        } catch (IOException e) {
            throw new CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
        }
    }
}
