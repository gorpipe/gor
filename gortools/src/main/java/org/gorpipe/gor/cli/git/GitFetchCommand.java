package org.gorpipe.gor.cli.git;

import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "fetch",
        description = "Download objects and refs from another repository.")
public class GitFetchCommand extends GitHelpOptions implements Runnable {

    @CommandLine.Parameters(index = "0", arity = "0..1", paramLabel = "REPOSITORY",
            description = "The repository to fetch from.")
    private String repository;

    @CommandLine.Parameters(index = "1", arity = "0..1", paramLabel = "REFSPEC",
            description = "The refs to fetch.")
    private String refspec;

    @CommandLine.Option(names = {"--all"},
            description = "Fetch all remotes.")
    private boolean all;

    @CommandLine.Option(names = {"-p", "--prune"},
            description = "Remove remote-tracking references that no longer exist on the remote.")
    private boolean prune;

    @CommandLine.Option(names = {"--tags"},
            description = "Fetch all tags from the remote.")
    private boolean tags;

    @CommandLine.Option(names = {"-q", "--quiet"},
            description = "Operate quietly.")
    private boolean quiet;

    @CommandLine.Option(names = {"-v", "--verbose"},
            description = "Be verbose.")
    private boolean verbose;

    @CommandLine.Option(names = {"--recurse-submodules"}, arity = "0..1", paramLabel = "MODE",
            description = "Fetch submodule changes. MODE: yes, on-demand (default), no.")
    private String recurseSubmodules;

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
            args.add("--all");
        }

        if (prune) {
            args.add("--prune");
        }

        if (tags) {
            args.add("--tags");
        }

        if (quiet) {
            args.add("-q");
        }

        if (verbose) {
            args.add("-v");
        }

        if (recurseSubmodules != null) {
            if (recurseSubmodules.isEmpty()) {
                args.add("--recurse-submodules");
            } else {
                args.add("--recurse-submodules=" + recurseSubmodules);
            }
        }

        if (repository != null) {
            args.add(parentCommand.getFullRepositoryPath(repository));
        }

        if (refspec != null) {
            args.add(refspec);
        }

        File workingDir = parentCommand.getWorkingDirectory(directory);

        GitCommandExecutor.executeGitCommand("fetch", args, workingDir, spec);
    }
}
