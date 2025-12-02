package org.gorpipe.gor.cli.git;

import org.gorpipe.gor.cli.GorExecCLI;
import org.gorpipe.gor.cli.HelpOptions;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.util.Strings;
import picocli.CommandLine;

import java.io.File;

@SuppressWarnings("squid:S106")
@CommandLine.Command(name = "git",
        description = "Wrapper for git commands (clone, checkout, pull, push, commit).",
        header = "Git command wrapper.",
        subcommands = {GitCloneCommand.class, GitCheckoutCommand.class, GitPullCommand.class, GitPushCommand.class, GitCommitCommand.class})
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
            var userPass = "%s:%s@".formatted(user, pass);
            return "https://%sgithub.com/GeneDx/%s.git".formatted(userPass, repository);
        } else {
            return "git@github.com:GeneDx/%s.git".formatted(repository);
        }
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

