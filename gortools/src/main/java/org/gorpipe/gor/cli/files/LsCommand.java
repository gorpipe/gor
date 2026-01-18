package org.gorpipe.gor.cli.files;

import picocli.CommandLine;

import java.io.IOException;

@CommandLine.Command(name = "ls", description = "List directory contents.")
public class LsCommand implements Runnable {

    @CommandLine.Parameters(index = "0", defaultValue = ".", paramLabel = "PATH")
    private String path;

    @CommandLine.Option(names = {"-r", "--recursive"}, description = "Recursively list contents.")
    private boolean recursive;

    @CommandLine.ParentCommand
    private FilesCommand parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        var reader = parent.getFileReader();
        try (var stream = recursive ? reader.walk(path) : reader.list(path)) {
            stream.forEach(parent.getStdOut()::println);
        } catch (IOException e) {
            throw new CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
        }
    }
}
