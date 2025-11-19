package org.gorpipe.gor.cli.link;

import org.gorpipe.gor.cli.HelpOptions;
import picocli.CommandLine;

@SuppressWarnings("squid:S106")
@CommandLine.Command(name = "link",
        description = "Manage link files (create, update, rollback).",
        header = "Link file management commands.",
        subcommands = {LinkUpdateCommand.class, LinkRollbackCommand.class, LinkResolveCommand.class})
public class LinkCommand extends HelpOptions implements Runnable {

    @Override
    public void run() {
        CommandLine.usage(this, System.err);
    }
}
