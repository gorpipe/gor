package org.gorpipe.gor.table.dictionary;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.table.livecycle.TableTwoPhaseCommitSupport;
import org.gorpipe.gor.table.util.PathUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URI;
import java.util.Iterator;

public class DictionaryTwoPhaseCommitSupport extends TableTwoPhaseCommitSupport {

    DictionaryTable table;

    private static final Logger log = LoggerFactory.getLogger(DictionaryTwoPhaseCommitSupport.class);
    public DictionaryTwoPhaseCommitSupport(DictionaryTable table) {
        super(table);
        this.table = table;
    }

    @Override
    protected void saveTempMainFile() {
        log.debug("Saving {} entries for table {}", table.tableEntries.size(), table.getName());

        try {
            String tempDict = getTempMainFileName();
            try (BufferedWriter writer = new BufferedWriter(
                    new OutputStreamWriter(table.getFileReader().getOutputStream(tempDict)))) {
                if (table.isUseEmbeddedHeader()) {
                    writer.write(table.formatHeader());
                }
                Iterator<DictionaryEntry> it = table.tableEntries.iterator();
                while (it.hasNext()) {
                    String line = it.next().formatEntryNoNewLine();
                    writer.write(line);
                    writer.newLine();
                }
            }

            if (!table.isUseEmbeddedHeader()) {
                URI tempHeader = URI.create(getTempFileName(PathUtils.resolve(table.getFolderPath(), "header")));
                try (BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(table.getFileReader().getOutputStream((tempHeader.toString()))))) {
                    writer.write(table.formatHeader());
                }
            }
        } catch (IOException e) {
            throw new GorSystemException(e);
        }
        log.debug("Done saving {} entries for table {}", table.tableEntries.size(), table.getName());
    }

    @Override
    public void commit() {
        super.commit();
        try {
            if (!table.isUseEmbeddedHeader()) {
                updateFromTempFile(PathUtils.resolve(table.getFolderPath(), "header"),
                        getTempFileName(PathUtils.resolve(table.getFolderPath(), "header")));
            }
        } catch (IOException e) {
            throw new GorSystemException("Could not move header", e);
        }
    }
}
