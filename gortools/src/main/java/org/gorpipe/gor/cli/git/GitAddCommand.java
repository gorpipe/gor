package org.gorpipe.gor.cli.git;

import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "add",
        description = "Add file contents to the staging area.")
public class GitAddCommand implements Runnable {

    @CommandLine.Parameters(arity = "0..*", paramLabel = "FILE",
            description = "Files to add to the staging area. Patterns can be used, e.g., '*.java'.")
    private List<String> files;

    @CommandLine.Option(names = {"-A", "--all"},
            description = "Stage all changes in the working tree.")
    private boolean all;

    @CommandLine.Option(names = {"-u", "--update"},
            description = "Stage only tracked files that have been modified or deleted.")
    private boolean update;

    @CommandLine.Option(names = {"-f", "--force"},
            description = "Allow adding otherwise ignored files.")
    private boolean force;

    @CommandLine.Option(names = {"-v", "--verbose"},
            description = "Be verbose.")
    private boolean verbose;

    @CommandLine.Option(names = {"-n", "--dry-run"},
            description = "Don't actually add the file(s), just show if they exist and/or will be ignored.")
    private boolean dryRun;

    @CommandLine.Option(names = {"-p", "--patch"},
            description = "Interactively choose hunks of patch between the index and the work tree.")
    private boolean patch;

    @CommandLine.Option(names = {"-d", "--directory"}, paramLabel = "DIRECTORY",
            description = "Directory to work in.", defaultValue = ".")
    private String directory;

    @CommandLine.ParentCommand
    private GitCommand parentCommand;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        List<String> args = new ArrayList<>();

        if (all) {
            args.add("-A");
        }

        if (update) {
            args.add("-u");
        }

        if (force) {
            args.add("-f");
        }

        if (verbose) {
            args.add("-v");
        }

        if (dryRun) {
            args.add("-n");
        }

        if (patch) {
            args.add("-p");
        }

        if (files != null && !files.isEmpty()) {
            args.addAll(files);
        }

        File workingDir = parentCommand.getWorkingDirectory(directory);

        GitCommandExecutor.executeGitCommand("add", args, workingDir, spec, parentCommand.out(), parentCommand.err());
    }
}

