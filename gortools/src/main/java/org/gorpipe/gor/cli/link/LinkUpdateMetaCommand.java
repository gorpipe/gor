package org.gorpipe.gor.cli.link;

import org.gorpipe.gor.cli.HelpOptions;
import org.gorpipe.gor.driver.linkfile.LinkFile;
import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.util.Strings;
import picocli.CommandLine;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@CommandLine.Command(name = "updateMeta",
        header = "Update metadata of a link file.",
        description = "Update header properties of a link file without adding a new entry.")
public class LinkUpdateMetaCommand extends HelpOptions implements Runnable {

    @CommandLine.Parameters(index = "0", paramLabel = "LINK_FILE", description = "Path to the link file.")
    private String linkFilePath;

    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    @CommandLine.Option(names = {"-h", "--header"}, paramLabel = "KEY=VALUE",
            description = "Header property to upsert in the link file metadata. Repeatable.",
            mapFallbackValue = "")
    private final Map<String, String> headerParams = new LinkedHashMap<>();

    @CommandLine.Option(names = {"-r", "--remove-header"}, paramLabel = "KEY",
            description = "Header property key to remove from the link file metadata. Repeatable.")
    private final List<String> removeHeaderKeys = new ArrayList<>();

    @CommandLine.ParentCommand
    private LinkCommand mainCmd;

    @Override
    public void run() {
        var normalizedLinkPath = LinkFile.validateAndUpdateLinkFileName(linkFilePath);
        try {
            var reader = new DriverBackedFileReader(mainCmd.getSecurityContext(), mainCmd.getProjectRoot());
            var linkFile = LinkFile.loadV1(LinkStreamSourceProvider.resolve(
                    normalizedLinkPath, mainCmd.getSecurityContext(), mainCmd.getProjectRoot(), true, this));
            applyHeaders(linkFile);
            removeHeaders(linkFile);
            linkFile.save(reader);
            System.err.printf("Updated meta for link file %s%n", normalizedLinkPath);
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

    private void removeHeaders(LinkFile linkFile) {
        for (var key : removeHeaderKeys) {
            if (!Strings.isNullOrEmpty(key)) {
                linkFile.getMeta().removeProperty(key.trim().toUpperCase());
            }
        }
    }
}
