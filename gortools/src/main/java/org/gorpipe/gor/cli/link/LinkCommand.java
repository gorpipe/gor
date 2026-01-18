package org.gorpipe.gor.cli.link;

import org.gorpipe.gor.cli.BaseSubCommand;
import picocli.CommandLine;

@SuppressWarnings("squid:S106")
@CommandLine.Command(name = "link",
        description = "Manage link files (create, update, rollback, list).",
        header = "Link file management commands.",
        subcommands = {LinkUpdateCommand.class, LinkRollbackCommand.class, LinkResolveCommand.class, LinkListCommand.class})
public class LinkCommand extends BaseSubCommand {

}
