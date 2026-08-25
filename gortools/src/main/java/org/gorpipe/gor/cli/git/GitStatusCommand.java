package org.gorpipe.gor.cli.git;

import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "status",
        description = "Show the working tree status.")
public class GitStatusCommand extends GitHelpOptions implements Runnable {

    @CommandLine.Option(names = {"-s", "--short"},
            description = "Give the output in the short-format.")
    private boolean shortFormat;

    @CommandLine.Option(names = {"-b", "--branch"},
            description = "Show the branch and tracking info even in short-format.")
    private boolean branch;

    @CommandLine.Option(names = {"--porcelain"},
            description = "Give the output in an easy-to-parse format for scripts.")
    private boolean porcelain;

    @CommandLine.Option(names = {"-u", "--untracked-files"}, arity = "0..1", paramLabel = "MODE",
            description = "Show untracked files. MODE: no, normal (default), all.")
    private String untrackedFiles;

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

        if (shortFormat) {
            args.add("-s");
        }

        if (branch) {
            args.add("-b");
        }

        if (porcelain) {
            args.add("--porcelain");
        }

        if (untrackedFiles != null) {
            args.add("-u" + untrackedFiles);
        }

        File workingDir = parentCommand.getWorkingDirectory(directory);

        GitCommandExecutor.executeGitCommand("status", args, workingDir, spec);
    }
}
