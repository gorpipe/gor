package org.gorpipe.gor.cli.git;

import picocli.CommandLine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Utility class for executing git commands.
 */
class GitCommandExecutor {

    /**
     * Execute a git command with the given arguments.
     *
     * @param gitSubcommand the git subcommand (e.g., "clone", "checkout", "pull", "push")
     * @param args additional arguments to pass to git
     * @param workingDir the working directory for the command (null for current directory)
     * @param commandSpec the CommandLine spec for error reporting
     * @param out the Print for standard output
     * @param err the PrintStream for standard error
     * @return the exit code of the git command
     */
    static int executeGitCommand(String gitSubcommand, List<String> args, File workingDir, 
                                  CommandLine.Model.CommandSpec commandSpec,
                                 PrintWriter out, PrintWriter err) {
        List<String> command = new ArrayList<>();
        command.add("git");
        command.add(gitSubcommand);
        command.addAll(args);

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            if (workingDir != null) {
                pb.directory(workingDir);
            }
            pb.redirectErrorStream(false);

            Process process = pb.start();

            streamOutput(process.getInputStream(), out);

            streamOutput(process.getErrorStream(), err);

            int exitCode = process.waitFor();

            if (exitCode != 0) {
                throw new CommandLine.ExecutionException(commandSpec.commandLine(),
                        String.format("Git command '%s' failed with exit code %d", gitSubcommand, exitCode));
            }

            return exitCode;
        } catch (CommandLine.ExecutionException e) {
            throw e;
        } catch (Exception e) {
            throw new CommandLine.ExecutionException(commandSpec.commandLine(),
                    String.format("Failed to execute git command '%s': %s", gitSubcommand, e.getMessage()), e);
        }
    }

    private static void streamOutput(InputStream inputStream, PrintWriter out) {
        new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    out.println(line);
                }
            } catch (Exception e) {
                // Ignore errors in output streaming
            }
        }).start();
    }
}

