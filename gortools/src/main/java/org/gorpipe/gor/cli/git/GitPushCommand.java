package org.gorpipe.gor.cli.git;

import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "push",
        description = "Update remote refs along with associated objects.")
public class GitPushCommand implements Runnable {

    @CommandLine.Parameters(index = "0", arity = "0..1", paramLabel = "REPOSITORY",
            description = "The repository to push to.")
    private String repository;

    @CommandLine.Parameters(index = "1", arity = "0..1", paramLabel = "REFSPEC",
            description = "The branch or ref to push.")
    private String refspec;

    @CommandLine.Option(names = {"-d", "--directory"}, paramLabel = "DIRECTORY",
            description = "Directory to work in.", defaultValue = ".")
    private String directory;

    @CommandLine.Option(names = {"-u", "--set-upstream"},
            description = "Set upstream for git pull/status.")
    private boolean setUpstream;

    @CommandLine.Option(names = {"--force", "-f"},
            description = "Force update remote refs.")
    private boolean force;

    @CommandLine.Option(names = {"--force-with-lease"},
            description = "Force update, but refuse to update if the remote-tracking ref has been updated.")
    private boolean forceWithLease;

    @CommandLine.Option(names = {"-q", "--quiet"},
            description = "Operate quietly.")
    private boolean quiet;

    @CommandLine.Option(names = {"-v", "--verbose"},
            description = "Be verbose.")
    private boolean verbose;

    @CommandLine.Option(names = {"--tags"},
            description = "Push all refs under refs/tags.")
    private boolean tags;

    @CommandLine.ParentCommand
    private GitCommand parentCommand;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        List<String> args = new ArrayList<>();

        if (setUpstream) {
            args.add("-u");
        }

        if (force) {
            args.add("--force");
        }

        if (forceWithLease) {
            args.add("--force-with-lease");
        }

        if (quiet) {
            args.add("-q");
        }

        if (verbose) {
            args.add("-v");
        }

        if (tags) {
            args.add("--tags");
        }

        if (repository != null) {
            args.add(parentCommand.getFullRepositoryPath(repository));
        }

        if (refspec != null) {
            args.add(refspec);
        }

        File workingDir = parentCommand.getWorkingDirectory(directory);

        GitCommandExecutor.executeGitCommand("push", args, workingDir, spec, parentCommand.out(), parentCommand.err());
    }
}

