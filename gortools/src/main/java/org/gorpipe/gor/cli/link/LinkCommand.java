package org.gorpipe.gor.cli.link;

import org.gorpipe.gor.cli.GorExecCLI;
import org.gorpipe.gor.cli.HelpOptions;
import picocli.CommandLine;

@SuppressWarnings("squid:S106")
@CommandLine.Command(name = "link",
        description = "Manage link files (create, update, rollback, list).",
        header = "Link file management commands.",
        subcommands = {LinkUpdateCommand.class, LinkRollbackCommand.class, LinkResolveCommand.class, LinkListCommand.class})
public class LinkCommand extends HelpOptions implements Runnable {

    @CommandLine.ParentCommand
    private GorExecCLI parentCommand;

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }

    public String getSecurityContext() {
        return parentCommand.getSecurityContext();
    }

    public String getProjectRoot() {
        return parentCommand.getProjectRoot();
    }
}
