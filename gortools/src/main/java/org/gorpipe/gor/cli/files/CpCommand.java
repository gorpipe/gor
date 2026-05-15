package org.gorpipe.gor.cli.files;

import org.gorpipe.gor.cli.HelpOptions;
import org.gorpipe.gor.model.FileReader;
import picocli.CommandLine;

import java.io.IOException;
import java.io.UncheckedIOException;

@CommandLine.Command(name = "cp", header = "Copy files.", description = "Copy files.")
public class CpCommand extends HelpOptions implements Runnable {

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

    @CommandLine.Option(names = {"-r", "-R", "--recursive"}, description = "Copy directories recursively.")
    private boolean recursive;

    @CommandLine.ParentCommand
    private FilesCommand parent;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        var reader = parent.getFileReader();
        try {
            if (recursive) {
                if (!reader.isDirectory(source)) {
                    throw new CommandLine.ExecutionException(spec.commandLine(), "cp: -r: '" + source + "' is not a directory");
                }
                try (var stream = reader.walk(source)) {
                    stream.forEach(srcPath -> {
                        String relPath = srcPath.equals(source) ? "" : srcPath.substring(source.length());
                        String destPath = dest + relPath;
                        try {
                            if (reader.isDirectory(srcPath)) {
                                reader.createDirectories(destPath);
                            } else if (shouldProceed(destPath, reader)) {
                                if (reader.exists(destPath)) reader.delete(destPath);
                                reader.copy(srcPath, destPath);
                                if (verbose) System.out.printf("'%s' -> '%s'%n", srcPath, destPath);
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                } catch (UncheckedIOException e) {
                    throw new CommandLine.ExecutionException(spec.commandLine(), e.getCause().getMessage(), e.getCause());
                }
            } else {
                if (shouldProceed(dest, reader)) {
                    if (reader.exists(dest)) reader.delete(dest);
                    reader.copy(source, dest);
                    if (verbose) System.out.printf("'%s' -> '%s'%n", source, dest);
                }
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
