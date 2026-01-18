package org.gorpipe.gor.cli.git;

import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "diff",
        description = "Show changes between commits, commit and working tree, etc.")
public class GitDiffCommand implements Runnable {

    @CommandLine.Option(names = {"--cached", "--staged"},
            description = "Show staged changes.")
    private boolean cached;

    @CommandLine.Option(names = {"--name-only"},
            description = "Show only names of changed files.")
    private boolean nameOnly;

    @CommandLine.Option(names = {"--name-status"},
            description = "Show names and status of changed files.")
    private boolean nameStatus;

    @CommandLine.Option(names = {"--stat"},
            description = "Show diffstat output.")
    private boolean stat;

    @CommandLine.Option(names = {"--numstat"},
            description = "Show number of added and deleted lines in decimal.")
    private boolean numstat;

    @CommandLine.Option(names = {"--shortstat"},
            description = "Show summary of changes.")
    private boolean shortstat;

    @CommandLine.Option(names = {"--color"},
            description = "Always show colored diff output.")
    private boolean color;

    @CommandLine.Option(names = {"-d", "--directory"}, paramLabel = "DIRECTORY",
            description = "Directory to work in.", defaultValue = ".")
    private String directory;

    @CommandLine.Parameters(arity = "0..*", paramLabel = "ARGS",
            description = "Additional arguments such as commit ranges or paths.")
    private List<String> args;

    @CommandLine.ParentCommand
    private GitCommand parentCommand;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        List<String> cmdArgs = new ArrayList<>();

        if (cached) {
            cmdArgs.add("--cached");
        }

        if (nameOnly) {
            cmdArgs.add("--name-only");
        }

        if (nameStatus) {
            cmdArgs.add("--name-status");
        }

        if (stat) {
            cmdArgs.add("--stat");
        }

        if (numstat) {
            cmdArgs.add("--numstat");
        }

        if (shortstat) {
            cmdArgs.add("--shortstat");
        }

        if (color) {
            cmdArgs.add("--color");
        }

        if (args != null && !args.isEmpty()) {
            cmdArgs.addAll(args);
        }

        File workingDir = parentCommand.getWorkingDirectory(directory);

        GitCommandExecutor.executeGitCommand("diff", cmdArgs, workingDir, spec, parentCommand.getStdOut(), parentCommand.getStdErr());
    }
}
