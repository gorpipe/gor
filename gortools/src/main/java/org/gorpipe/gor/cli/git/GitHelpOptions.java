package org.gorpipe.gor.cli.git;

import picocli.CommandLine;

class GitHelpOptions {
    @CommandLine.Option(names = {"-h", "--help"}, hidden = true, usageHelp = true,
            description = "Print usage help and exit.")
    boolean usageHelpRequested;
}
