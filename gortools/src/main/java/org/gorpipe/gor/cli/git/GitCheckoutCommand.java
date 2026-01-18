package org.gorpipe.gor.cli.git;

import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "checkout",
        description = "Switch branches or restore working tree files.")
public class GitCheckoutCommand implements Runnable {

    @CommandLine.Parameters(index = "0", arity = "1..*", paramLabel = "BRANCH_OR_FILE",
            description = "Branch, commit or files to checkout.")
    private List<String> branchOrFiles;

    @CommandLine.Option(names = {"-d", "--directory"}, paramLabel = "DIRECTORY",
            description = "Directory to work in.", defaultValue = ".")
    private String directory;

    @CommandLine.Option(names = {"-b", "--branch"}, paramLabel = "NEW_BRANCH",
            description = "Create a new branch and switch to it.")
    private String newBranch;

    @CommandLine.Option(names = {"-B", "--force-branch"}, paramLabel = "NEW_BRANCH",
            description = "Create a new branch or reset an existing branch and switch to it.")
    private String forceBranch;

    @CommandLine.Option(names = {"-f", "--force"},
            description = "Force checkout (throw away local changes).")
    private boolean force;

    @CommandLine.Option(names = {"-q", "--quiet"},
            description = "Suppress feedback messages.")
    private boolean quiet;

    @CommandLine.ParentCommand
    private GitCommand parentCommand;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        if (branchOrFiles == null || branchOrFiles.isEmpty()) {
            throw new CommandLine.ParameterException(spec.commandLine(),
                    "At least one branch or file must be specified.");
        }

        List<String> args = new ArrayList<>();

        if (newBranch != null) {
            args.add("-b");
            args.add(newBranch);
        }

        if (forceBranch != null) {
            args.add("-B");
            args.add(forceBranch);
        }

        if (force) {
            args.add("-f");
        }

        if (quiet) {
            args.add("-q");
        }

        args.addAll(branchOrFiles);

        File workingDir = parentCommand.getWorkingDirectory(directory);

        GitCommandExecutor.executeGitCommand("checkout", args, workingDir, spec, parentCommand.getStdOut(), parentCommand.getStdErr());
    }
}

