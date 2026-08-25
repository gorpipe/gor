package org.gorpipe.gor.cli.git;

import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@CommandLine.Command(name = "log",
        description = "Show commit logs.")
public class GitLogCommand extends GitHelpOptions implements Runnable {

    @CommandLine.Option(names = {"-n", "--max-count"}, paramLabel = "NUMBER",
            description = "Limit the number of commits to output.")
    private Integer maxCount;

    @CommandLine.Option(names = {"--oneline"},
            description = "Shorthand for --pretty=oneline --abbrev-commit.")
    private boolean oneline;

    @CommandLine.Option(names = {"--graph"},
            description = "Draw a text-based graphical representation of the commit history.")
    private boolean graph;

    @CommandLine.Option(names = {"-p", "--patch"},
            description = "Generate patch output.")
    private boolean patch;

    @CommandLine.Option(names = {"--stat"},
            description = "Generate a diffstat.")
    private boolean stat;

    @CommandLine.Option(names = {"--all"},
            description = "Pretend as if all refs in refs/ are listed on the command line.")
    private boolean all;

    @CommandLine.Option(names = {"--follow"},
            description = "Continue listing the history of a file beyond renames.")
    private boolean follow;

    @CommandLine.Option(names = {"--author"}, paramLabel = "PATTERN",
            description = "Limit commits to those with author matching the given pattern.")
    private String author;

    @CommandLine.Option(names = {"--since", "--after"}, paramLabel = "DATE",
            description = "Show commits more recent than a specific date.")
    private String since;

    @CommandLine.Option(names = {"--until", "--before"}, paramLabel = "DATE",
            description = "Show commits older than a specific date.")
    private String until;

    @CommandLine.Option(names = {"-d", "--directory"}, paramLabel = "DIRECTORY",
            description = "Directory to work in.", defaultValue = ".")
    private String directory;

    @CommandLine.Parameters(arity = "0..*", paramLabel = "ARGS",
            description = "Commit ranges or paths to limit log output.")
    private List<String> args;

    @CommandLine.ParentCommand
    private GitCommand parentCommand;

    @CommandLine.Spec
    private CommandLine.Model.CommandSpec spec;

    @Override
    public void run() {
        List<String> cmdArgs = new ArrayList<>();

        if (maxCount != null) {
            cmdArgs.add("-n");
            cmdArgs.add(maxCount.toString());
        }

        if (oneline) {
            cmdArgs.add("--oneline");
        }

        if (graph) {
            cmdArgs.add("--graph");
        }

        if (patch) {
            cmdArgs.add("-p");
        }

        if (stat) {
            cmdArgs.add("--stat");
        }

        if (all) {
            cmdArgs.add("--all");
        }

        if (follow) {
            cmdArgs.add("--follow");
        }

        if (author != null) {
            cmdArgs.add("--author=" + author);
        }

        if (since != null) {
            cmdArgs.add("--since=" + since);
        }

        if (until != null) {
            cmdArgs.add("--until=" + until);
        }

        if (args != null && !args.isEmpty()) {
            cmdArgs.addAll(args);
        }

        File workingDir = parentCommand.getWorkingDirectory(directory);

        GitCommandExecutor.executeGitCommand("log", cmdArgs, workingDir, spec);
    }
}
