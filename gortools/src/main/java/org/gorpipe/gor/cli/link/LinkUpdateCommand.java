package org.gorpipe.gor.cli.link;

import org.gorpipe.gor.driver.linkfile.LinkFile;
import org.gorpipe.util.Strings;
import picocli.CommandLine;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@CommandLine.Command(name = "update",
        description = "Append a new entry to a link file, creating the file if needed.")
public class LinkUpdateCommand implements Runnable {

    @CommandLine.Parameters(index = "0", paramLabel = "LINK_FILE", description = "Path to the link file to update.")
    private String linkFilePath;

    @CommandLine.Parameters(index = "1", paramLabel = "LINK_VALUE", description = "Value to add to the link file (file path, URL or query).")
    private String linkValue;

    @CommandLine.Option(names = {"-m", "--md5"}, paramLabel = "MD5",
            description = "MD5 checksum to associate with the new link entry.")
    private String entryMd5;

    @CommandLine.Option(names = {"-i", "--info"}, paramLabel = "INFO",
            description = "Free-form info string to store with the new link entry.")
    private String entryInfo;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @CommandLine.Option(names = {"-h", "--header"}, paramLabel = "KEY=VALUE",
            description = "Header property to upsert in the link file metadata. Repeatable.",
            mapFallbackValue = "")
    private final Map<String, String> headerParams = new LinkedHashMap<>();

    @CommandLine.ParentCommand
    private LinkCommand mainCmd;

    @Override
    public void run() {
        var normalizedLinkPath = LinkFile.validateAndUpdateLinkFileName(linkFilePath);
        try {
            var linkFile = LinkFile.load(LinkStreamSourceProvider.resolve(normalizedLinkPath, mainCmd.getSecurityContext(), mainCmd.getProjectRoot(),  true, this));
            applyHeaders(linkFile);
            linkFile.appendEntry(linkValue, entryMd5, entryInfo);
            linkFile.save();
            System.err.printf("Updated link file %s with %s%n", normalizedLinkPath, linkValue);
        } catch (IOException e) {
            throw new CommandLine.ExecutionException(new CommandLine(this),
                    "Failed to load or create link file: " + normalizedLinkPath, e);
        }
    }

    private void applyHeaders(LinkFile linkFile) {
        for (var entry : headerParams.entrySet()) {
            var key = entry.getKey();
            var value = entry.getValue();
            if (Strings.isNullOrEmpty(key)) {
                continue;
            }
            linkFile.getMeta().setProperty(key.trim().toUpperCase(), value != null ? value.trim() : "");
        }
    }
}

