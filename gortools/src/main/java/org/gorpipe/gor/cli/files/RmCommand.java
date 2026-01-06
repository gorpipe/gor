package org.gorpipe.gor.cli.files;

import java.io.IOException;
import java.util.List;

import picocli.CommandLine;

@CommandLine.Command(name = "rm", description = "Remove files or directories.")
public class RmCommand implements Runnable {

    @CommandLine.Option(names = {"-r", "--recursive"}, description = "Recursively delete directories.")
    private boolean recursive;

    @CommandLine.Parameters(arity = "1..*", paramLabel = "PATH")
    private List<String> paths;

    @CommandLine.ParentCommand
    private FilesCommand parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        var reader = parent.getFileReader();
        for (var path : paths) {
            try {
                if (recursive) {
                    reader.deleteDirectory(path);
                } else if (reader.isDirectory(path)) {
                    throw new CommandLine.ExecutionException(spec.commandLine(), "Use -r to delete directory: " + path);
                } else {
                    reader.delete(path);
                }
            } catch (IOException e) {
                throw new CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
            }
        }
    }
}
