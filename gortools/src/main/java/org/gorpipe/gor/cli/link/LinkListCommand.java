package org.gorpipe.gor.cli.link;

import org.gorpipe.gor.driver.linkfile.LinkFile;
import org.gorpipe.util.Strings;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

@CommandLine.Command(name = "list",
        description = "List the raw content of a link file without resolving entries.")
public class LinkListCommand implements Runnable {

    @CommandLine.Parameters(index = "0", paramLabel = "LINK_FILE",
            description = "Path to the link file to list.")
    private String linkFilePath;

    @CommandLine.Option(names = {"-r", "--raw-output"},
            description = "Return the full link file content without any processing, include header")
    private boolean rawOutput;

    @CommandLine.ParentCommand
    private LinkCommand mainCmd;

    @Override
    public void run() {
        var normalizedLinkPath = LinkFile.validateAndUpdateLinkFileName(linkFilePath);
        try {
            var source = LinkStreamSourceProvider.resolve(normalizedLinkPath, mainCmd.getSecurityContext(), mainCmd.getProjectRoot(),  true, this);
            var content = LinkFile.loadContentFromSource(source);
            if (rawOutput) {
                System.out.println(content.replace("\t", "    "));
            } else {
                for (var line : content.split("\n")) {
                    if (line.startsWith("##")) {
                        continue;
                    }
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            throw new CommandLine.ExecutionException(new CommandLine(this),
                    "Failed to read link file: " + normalizedLinkPath, e);
        }
    }
}
