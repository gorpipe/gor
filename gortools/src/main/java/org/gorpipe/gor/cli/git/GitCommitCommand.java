package org.gorpipe.gor.cli.git;

import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "commit",
        description = "Record changes to the repository.")
public class GitCommitCommand implements Runnable {

    @CommandLine.Option(names = {"-m", "--message"}, paramLabel = "MESSAGE",
            description = "Use the given message as the commit message.")
    private String message;

    @CommandLine.Option(names = {"-a", "--all"},
            description = "Automatically stage files that have been modified and deleted.")
    private boolean all;

    @CommandLine.Option(names = {"--amend"},
            description = "Amend the previous commit.")
    private boolean amend;

    @CommandLine.Option(names = {"--no-verify", "-n"},
            description = "Bypass pre-commit and commit-msg hooks.")
    private boolean noVerify;

    @CommandLine.Option(names = {"-v", "--verbose"},
            description = "Show unified diff of all file changes in the commit message template.")
    private boolean verbose;

    @CommandLine.Option(names = {"-q", "--quiet"},
            description = "Suppress commit summary message.")
    private boolean quiet;

    @CommandLine.Option(names = {"-d", "--directory"}, paramLabel = "DIRECTORY",
            description = "Directory to work in.", defaultValue = ".")
    private String directory;

    @CommandLine.Parameters(arity = "0..*", paramLabel = "FILE",
            description = "Files to commit. If not specified, all staged files are committed.")
    private List<String> files;

    @CommandLine.ParentCommand
    private GitCommand parentCommand;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        List<String> args = new ArrayList<>();

        if (all) {
            args.add("-a");
        }

        if (amend) {
            args.add("--amend");
        }

        if (noVerify) {
            args.add("--no-verify");
        }

        if (verbose) {
            args.add("-v");
        }

        if (quiet) {
            args.add("-q");
        }

        if (message != null) {
            args.add("-m");
            args.add(message);
        }

        if (files != null && !files.isEmpty()) {
            args.addAll(files);
        }

        File workingDir = parentCommand.getWorkingDirectory(directory);

        GitCommandExecutor.executeGitCommand("commit", args, workingDir, spec, parentCommand.getStdOut(), parentCommand.getStdErr());
    }
}

