package org.gorpipe.gor.cli.link;

import java.io.IOException;
import java.time.Instant;

import org.gorpipe.gor.driver.linkfile.LinkFile;
import org.gorpipe.util.DateUtils;

import picocli.CommandLine;

@CommandLine.Command(name = "rollback",
        description = "Rollback the latest entry or rollback entries newer than a given date.")
public class LinkRollbackCommand implements Runnable {

    @CommandLine.Parameters(index = "0", paramLabel = "LINK_FILE", description = "Path to the link file to rollback.")
    private String linkFilePath;

    @CommandLine.Option(names = {"-d", "--date"}, paramLabel = "DATE",
            description = "ISO-8601 date/time or epoch milliseconds to rollback to (entries newer than this are removed).")
    private String rollbackDate;

    @Override
    public void run() {
        var normalizedLinkPath = LinkFile.validateAndUpdateLinkFileName(linkFilePath);
        try {
            var linkFile = LinkFile.load(LinkStreamSourceProvider.resolve(normalizedLinkPath, true, this));
            boolean changed = rollbackDate == null ? linkFile.rollbackLatestEntry() : linkFile.rollbackToTimestamp(parseDate(rollbackDate));
            if (!changed) {
                throw new CommandLine.ParameterException(new CommandLine(this),
                        "No entries were removed. Link file may already be at the requested state.");
            }
            linkFile.save();
            System.err.printf("Rolled back link file %s%n", normalizedLinkPath);
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

