package org.gorpipe.gor.cli.git;

import org.gorpipe.gor.cli.GorExecCLI;
import org.gorpipe.gor.cli.HelpOptions;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.util.Strings;
import picocli.CommandLine;

import java.io.File;

@SuppressWarnings("squid:S106")
@CommandLine.Command(name = "git",
        description = "Wrapper for git commands (clone, checkout, pull, push, commit, add, config, diff, status, log, fetch).",
        header = "Git command wrapper.",
        subcommands = {GitCloneCommand.class, GitCheckoutCommand.class, GitPullCommand.class, GitPushCommand.class, GitCommitCommand.class, GitAddCommand.class, GitConfigCommand.class, GitDiffCommand.class, GitStatusCommand.class, GitLogCommand.class, GitFetchCommand.class})
public class GitCommand extends HelpOptions implements Runnable {

    @CommandLine.ParentCommand
    private GorExecCLI parentCommand;

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }

    public String getSecurityContext() {
        return parentCommand != null ? parentCommand.getSecurityContext() : "";
    }

    public String getProjectRoot() {
        return parentCommand != null ? parentCommand.getProjectRoot() : "";
    }

    public String getFullRepositoryPath(String repository) {
        if (repository.equals("origin")) {
            return repository;
        }
        String user = System.getenv("GOR_GIT_USER");
        String pass = System.getenv("GOR_GIT_TOKEN");
        if (user != null && pass != null) {
            return "https://%s:%s@github.com/GeneDx/%s.git"
                    .formatted(user, GitCommandExecutor.TOKEN_PLACEHOLDER, repository);
        }
        return "git@github.com:GeneDx/%s.git".formatted(repository);
    }

    public String getGitToken() {
        return System.getenv("GOR_GIT_TOKEN");
    }

    public File getWorkingDirectory(String directory) {
        File workingDir = null;
        if (!Strings.isNullOrEmpty(directory)) {

            if (!Strings.isNullOrEmpty(parentCommand.getProjectRoot())) {
                workingDir = new File(PathUtils.resolve(parentCommand.getProjectRoot(), directory).toString());
            } else {
                workingDir = new File(directory);
            }
        }
        return workingDir;
    }
}

