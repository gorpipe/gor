package org.gorpipe.gor.table.dictionary;

import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.table.livecycle.TableTwoPhaseCommitSupport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Iterator;

public class DictionaryTwoPhaseCommitSupport<T extends DictionaryEntry> extends TableTwoPhaseCommitSupport {

    DictionaryTable<T> table;

    private static final Logger log = LoggerFactory.getLogger(DictionaryTwoPhaseCommitSupport.class);
    public DictionaryTwoPhaseCommitSupport(DictionaryTable<T> table) {
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
                Iterator<T> it = table.tableEntries.iterator();
                while (it.hasNext()) {
                    String line = it.next().formatEntryNoNewLine();
                    writer.write(line);
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            throw new GorSystemException(e);
        }
        log.debug("Done saving {} entries for table {}", table.tableEntries.size(), table.getName());
    }
}
