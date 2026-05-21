package org.gorpipe.gor.cli.git;

import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility class for executing git commands.
 */
class GitCommandExecutor {

    static final String TOKEN_PLACEHOLDER = "__GIT_TOKEN__";

    /**
     * Execute a git command with the given arguments.
     *
     * @param gitSubcommand the git subcommand (e.g., "clone", "checkout", "pull", "push")
     * @param args additional arguments to pass to git
     * @param workingDir the working directory for the command (null for current directory)
     * @param commandSpec the CommandLine spec for error reporting
     * @return the exit code of the git command
     */
    static int executeGitCommand(String gitSubcommand, List<String> args, File workingDir,
                                  CommandLine.Model.CommandSpec commandSpec) {
        return executeGitCommand(gitSubcommand, args, workingDir, commandSpec, null);
    }

    static int executeGitCommand(String gitSubcommand, List<String> args, File workingDir,
                                  CommandLine.Model.CommandSpec commandSpec, String token) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add(gitSubcommand);
        command.addAll(args);

        // Resolve token placeholder just before exec — command retains placeholder for safe error messages
        List<String> execCommand = token != null
                ? command.stream().map(a -> a.replace(TOKEN_PLACEHOLDER, token)).collect(Collectors.toList())
                : command;

        try {
            ProcessBuilder pb = new ProcessBuilder(execCommand);
            if (workingDir != null && workingDir.isDirectory()) {
                pb.directory(workingDir);
            }
            pb.redirectErrorStream(false);

            Process process = pb.start();

            Thread outThread = streamOutput(process.getInputStream(), System.out);
            Thread errThread = streamOutput(process.getErrorStream(), System.err);

            int exitCode = process.waitFor();
            outThread.join();
            errThread.join();

            if (exitCode != 0) {
                throw new CommandLine.ExecutionException(commandSpec.commandLine(),
                        String.format("Git command '%s' failed with exit code %d (%s)", gitSubcommand, exitCode, String.join(" ", command)));
            }

            return exitCode;
        } catch (CommandLine.ExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new CommandLine.ExecutionException(commandSpec.commandLine(),
                    String.format("Failed to execute git command '%s': %s", gitSubcommand, e.getMessage()), e);
        }
    }

    private static Thread streamOutput(InputStream inputStream, java.io.PrintStream outputStream) {
        Thread t = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    outputStream.println(line);
                }
            } catch (Exception e) {
                // Ignore errors in output streaming
            }
        });
        t.start();
        return t;
    }
}

