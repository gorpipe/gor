package org.gorpipe.gor.cli.link;

import org.gorpipe.gor.cli.BaseSubCommand;
import org.gorpipe.gor.driver.linkfile.LinkFile;
import org.gorpipe.util.DateUtils;
import org.gorpipe.util.Strings;
import picocli.CommandLine;

import java.io.IOException;
import java.time.Instant;

@CommandLine.Command(name = "resolve",
        description = "Resolve a link file to the entry active at the current or given time.")
public class LinkResolveCommand extends BaseSubCommand implements Runnable {

    @CommandLine.Parameters(index = "0", paramLabel = "LINK_FILE",
            description = "Path to the link file to resolve.")
    private String linkFilePath;

    @CommandLine.Option(names = {"-d", "--date"}, paramLabel = "DATE",
            description = "ISO-8601 date/time or epoch milliseconds to resolve at (default: now).")
    private String resolveDate;

    @CommandLine.Option(names = {"-f", "--full-entry"},
            description = "Return the full link file entry instead of only the resolved URL.")
    private boolean returnFullEntry;

    @Override
    public void run() {
        var normalizedLinkPath = LinkFile.validateAndUpdateLinkFileName(linkFilePath);
        try {
            var linkFile = LinkFile.load(LinkStreamSourceProvider.resolve(normalizedLinkPath,
                    getSecurityContext(), getProjectRoot(),  true, this));
            long timestamp = resolveDate == null ? System.currentTimeMillis() : parseDate(resolveDate);
            var entry = linkFile.getEntry(timestamp);
            if (entry == null) {
                throw new CommandLine.ParameterException(new CommandLine(this),
                        "No link entry found for the requested time.");
            }
            String output;
            if (returnFullEntry) {
                output = entry.format().replace('\t', ' ');
            } else {
                var resolved = linkFile.getEntryUrl(timestamp);
                if (Strings.isNullOrEmpty(resolved)) {
                    throw new CommandLine.ParameterException(new CommandLine(this),
                            "No link entry found for the requested time.");
                }
                output = resolved;
            }
            out().println(output);
        } catch (IOException e) {
            throw new CommandLine.ExecutionException(new CommandLine(this),
                    "Failed to load link file: " + normalizedLinkPath, e);
        }
    }

    private long parseDate(String dateValue) {
        try {
            Instant instant = DateUtils.parseDateISOEpoch(dateValue, true);
            return instant.toEpochMilli();
        } catch (Exception e) {
            throw new CommandLine.ParameterException(new CommandLine(this),
                    "Invalid date value: " + dateValue, e);
        }
    }
}

