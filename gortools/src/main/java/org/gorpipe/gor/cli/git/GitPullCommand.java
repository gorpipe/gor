package org.gorpipe.gor.cli.git;

import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "pull",
        description = "Fetch from and integrate with another repository or a local branch.")
public class GitPullCommand implements Runnable {

    @CommandLine.Parameters(index = "0", arity = "0..1", paramLabel = "REPOSITORY",
            description = "The repository to pull from.")
    private String repository;

    @CommandLine.Option(names = {"-d", "--directory"}, paramLabel = "DIRECTORY",
            description = "Directory to work in.", defaultValue = ".")
    private String directory;

    @CommandLine.Parameters(index = "1", arity = "0..1", paramLabel = "REFSPEC",
            description = "The branch or commit to pull.")
    private String refspec;

    @CommandLine.Option(names = {"--rebase"},
            description = "Rebase the current branch on top of the upstream branch.")
    private boolean rebase;

    @CommandLine.Option(names = {"--no-rebase"},
            description = "Merge the remote-tracking branch into the current branch (default).")
    private boolean noRebase;

    @CommandLine.Option(names = {"--ff-only"},
            description = "Refuse to merge unless the current HEAD is already up to date or the merge can be resolved as a fast-forward.")
    private boolean ffOnly;

    @CommandLine.Option(names = {"--no-ff"},
            description = "Create a merge commit even when the merge could be resolved as a fast-forward.")
    private boolean noff;

    @CommandLine.Option(names = {"-q", "--quiet"},
            description = "Operate quietly.")
    private boolean quiet;

    @CommandLine.Option(names = {"-v", "--verbose"},
            description = "Be verbose.")
    private boolean verbose;

    @CommandLine.ParentCommand
    private GitCommand parentCommand;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        List<String> args = new ArrayList<>();

        if (rebase) {
            args.add("--rebase");
        }

        if (noRebase) {
            args.add("--no-rebase");
        }

        if (ffOnly) {
            args.add("--ff-only");
        }

        if (noff) {
            args.add("--no-ff");
        }

        if (quiet) {
            args.add("-q");
        }

        if (verbose) {
            args.add("-v");
        }

        if (repository != null) {
            args.add(parentCommand.getFullRepositoryPath(repository));
        }

        if (refspec != null) {
            args.add(refspec);
        }

        File workingDir = parentCommand.getWorkingDirectory(directory);

        GitCommandExecutor.executeGitCommand("pull", args, workingDir, spec, parentCommand.out(), parentCommand.err());
    }
}

