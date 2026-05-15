package org.gorpipe.gor.cli.files;

import org.gorpipe.gor.cli.HelpOptions;
import picocli.CommandLine;

import java.io.IOException;
import java.util.List;

@CommandLine.Command(name = "rm", header = "Remove files or directories.", description = "Remove files or directories.")
public class RmCommand extends HelpOptions implements Runnable {

    @CommandLine.Option(names = {"-r", "--recursive"}, description = "Recursively delete directories.")
    private boolean recursive;

    @CommandLine.Option(names = {"-f", "--force"}, description = "Ignore nonexistent files, never prompt.")
    private boolean force;

    @CommandLine.Option(names = {"-v", "--verbose"}, description = "Explain what is being done.")
    private boolean verbose;

    @CommandLine.Option(names = {"-d", "--dir"}, description = "Remove empty directories.")
    private boolean dir;

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
                if (!reader.exists(path)) {
                    if (!force) {
                        throw new CommandLine.ExecutionException(spec.commandLine(),
                                "rm: cannot remove '" + path + "': No such file or directory");
                    }
                    continue;
                }

                if (reader.isDirectory(path)) {
                    if (recursive) {
                        reader.deleteDirectory(path);
                    } else if (dir) {
                        try (var contents = reader.list(path)) {
                            if (contents.findAny().isPresent()) {
                                throw new CommandLine.ExecutionException(spec.commandLine(),
                                        "rm: cannot remove '" + path + "': Directory not empty");
                            }
                        }
                        reader.deleteDirectory(path);
                    } else {
                        throw new CommandLine.ExecutionException(spec.commandLine(),
                                "Use -r to delete directory: " + path);
                    }
                } else {
                    reader.delete(path);
                }

                if (verbose) System.out.printf("removed '%s'%n", path);
            } catch (IOException e) {
                throw new CommandLine.ExecutionException(spec.commandLine(), e.getMessage(), e);
            }
        }
    }
}
