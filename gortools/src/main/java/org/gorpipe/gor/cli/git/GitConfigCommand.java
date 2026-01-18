package org.gorpipe.gor.cli.git;

import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "config",
        description = "Get and set repository or global options.")
public class GitConfigCommand implements Runnable {

    @CommandLine.Parameters(arity = "0..*", paramLabel = "KEY [VALUE]",
            description = "Configuration key, optionally followed by value to set.")
    private List<String> configArgs;

    @CommandLine.Option(names = {"--global"},
            description = "Use global config file.")
    private boolean global;

    @CommandLine.Option(names = {"--local"},
            description = "Use repository config file (default).")
    private boolean local;

    @CommandLine.Option(names = {"--system"},
            description = "Use system config file.")
    private boolean system;

    @CommandLine.Option(names = {"--list", "-l"},
            description = "List all variables set in config file.")
    private boolean list;

    @CommandLine.Option(names = {"--get"},
            description = "Get the value for a given key.")
    private boolean get;

    @CommandLine.Option(names = {"--set"},
            description = "Set a value for a given key.")
    private boolean set;

    @CommandLine.Option(names = {"--unset"},
            description = "Remove a key from the config file.")
    private boolean unset;

    @CommandLine.Option(names = {"--unset-all"},
            description = "Remove all matches for a key from the config file.")
    private boolean unsetAll;

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

        if (global) {
            args.add("--global");
        }

        if (local) {
            args.add("--local");
        }

        if (system) {
            args.add("--system");
        }

        if (list) {
            args.add("--list");
        }

        if (get) {
            args.add("--get");
        }

        if (set) {
            args.add("--set");
        }

        if (unset) {
            args.add("--unset");
        }

        if (unsetAll) {
            args.add("--unset-all");
        }

        if (configArgs != null && !configArgs.isEmpty()) {
            args.addAll(configArgs);
        }

        File workingDir = parentCommand.getWorkingDirectory(directory);

        GitCommandExecutor.executeGitCommand("config", args, workingDir, spec, parentCommand.out(), parentCommand.err());
    }
}

