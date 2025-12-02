package org.gorpipe.gor.cli.git;

import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "clone",
        description = "Clone a repository into a new directory.")
public class GitCloneCommand implements Runnable {

    @CommandLine.Parameters(index = "0", paramLabel = "REPOSITORY",
            description = "The repository to clone from, just the repository name, e.g. ref-gregor.")
    private String repository;

    @CommandLine.Parameters(index = "1", arity = "0..1", paramLabel = "DIRECTORY",
            description = "The name of a new directory to clone into.")
    private String directory;

    @CommandLine.Option(names = {"--depth"}, paramLabel = "DEPTH",
            description = "Create a shallow clone with a history truncated to the specified number of commits.")
    private Integer depth;

    @CommandLine.Option(names = {"-b", "--branch"}, paramLabel = "BRANCH",
            description = "Clone a specific branch instead of the branch pointed to by HEAD.")
    private String branch;

    @CommandLine.Option(names = {"--single-branch"},
            description = "Clone only the history leading to the tip of a single branch.")
    private boolean singleBranch;

    @CommandLine.Option(names = {"--no-checkout"},
            description = "Do not checkout HEAD after cloning is complete.")
    private boolean noCheckout;

    @CommandLine.Option(names = {"--bare"},
            description = "Make a bare Git repository.")
    private boolean bare;

    @CommandLine.ParentCommand
    private GitCommand parentCommand;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        List<String> args = new ArrayList<>();

        if (depth != null) {
            args.add("--depth");
            args.add(depth.toString());
        }

        if (branch != null) {
            args.add("-b");
            args.add(branch);
        }

        if (singleBranch) {
            args.add("--single-branch");
        }

        if (noCheckout) {
            args.add("--no-checkout");
        }

        if (bare) {
            args.add("--bare");
        }

        args.add(parentCommand.getFullRepositoryPath(repository));

        if (directory != null) {
            args.add(directory);
        }

        File workingDir = parentCommand.getWorkingDirectory(".");

        GitCommandExecutor.executeGitCommand("clone", args, workingDir, spec);
    }
}

