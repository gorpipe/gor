package org.gorpipe.gor.table;

import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.table.dictionary.DictionaryTable;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A commit publishes the main file (lines) and the meta file (header, serial) with two separate moves.  These tests
 * run a reader exactly between the two moves and check that it never sees a serial that is newer than the content.
 */
public class UTestTableCommitOrdering {

    private static final int SAVES = 10;

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private Path root;
    private Path tablePath;

    @Before
    public void setUp() throws IOException {
        root = tmp.getRoot().toPath();
        tablePath = root.resolve("commitorder.gord");
        for (int i = 0; i < SAVES; i++) {
            Files.writeString(root.resolve("f" + i + ".gor"), "Chrom\tPos\n");
        }
    }

    @Test
    public void readerNeverSeesSerialNewerThanContent() throws IOException {
        WindowHookFileReader hookReader = new WindowHookFileReader(tablePath.getFileName().toString());
        DictionaryTable writer = createWriter(hookReader);
        writer.save();
        long baseSerial = serial(writer);

        DictionaryTable reader = new GorDictionaryTable(tablePath);
        List<String> violations = new ArrayList<>();
        hookReader.setHook(() -> {
            reader.reload();
            long serial = serial(reader);
            int lines = reader.getEntries().size();
            if (lines < serial - baseSerial) {
                violations.add("serial " + serial + " but only " + lines + " lines");
            }
        });

        for (int i = 0; i < SAVES; i++) {
            writer.insert("f" + i + ".gor\tPN" + i);
            hookReader.arm();
            writer.save();
            Assert.assertTrue("Hook should have run inside the commit", hookReader.hasRun());
        }

        Assert.assertEquals("Reader saw a serial newer than the content", List.of(), violations);
    }

    @Test
    public void tableReaderCacheIsNotStaleAfterCommit() throws IOException {
        WindowHookFileReader hookReader = new WindowHookFileReader(tablePath.getFileName().toString());
        DictionaryTable writer = createWriter(hookReader);
        writer.save();

        // Long-lived reader (like the table service's tables), it only drops its entries when the serial changes.
        DictionaryTable reader = new GorDictionaryTable(tablePath);
        reader.reload();
        Assert.assertEquals(0, reader.getEntries().size());

        hookReader.setHook(() -> {
            reader.reload();
            reader.getEntries();
        });
        writer.insert("f0.gor\tPN0");
        hookReader.arm();
        writer.save();
        Assert.assertTrue("Hook should have run inside the commit", hookReader.hasRun());

        reader.reload();
        Assert.assertEquals("Reader kept stale entries after the commit", 1, reader.getEntries().size());
    }

    @Test
    public void readerInWindowOfFirstSaveDoesNotFail() throws IOException {
        WindowHookFileReader hookReader = new WindowHookFileReader(tablePath.getFileName().toString());
        DictionaryTable writer = createWriter(hookReader);
        writer.insert("f0.gor\tPN0");

        List<String> violations = new ArrayList<>();
        hookReader.setHook(() -> {
            DictionaryTable reader = new GorDictionaryTable(tablePath);
            String serial = reader.getProperty(TableHeader.HEADER_SERIAL_KEY);
            int lines = reader.getEntries().size();
            if (serial != null && !TableHeader.NO_SERIAL.equals(serial) && lines < Long.parseLong(serial)) {
                violations.add("serial " + serial + " but only " + lines + " lines");
            }
        });
        hookReader.arm();
        writer.save();
        Assert.assertTrue("Hook should have run inside the commit", hookReader.hasRun());

        Assert.assertEquals("Reader saw a serial newer than the content", List.of(), violations);
    }

    private DictionaryTable createWriter(WindowHookFileReader fileReader) {
        return new GorDictionaryTable.Builder<>(tablePath.toUri().toString()).fileReader(fileReader).build();
    }

    private static long serial(DictionaryTable table) {
        return Long.parseLong(table.getProperty(TableHeader.HEADER_SERIAL_KEY));
    }

    /**
     * File reader that runs a hook right after the first move onto the table's main or meta file, once per arm().
     * That is inside the commit, between its two moves.  Other moves (e.g. the history log) don't trigger it.
     */
    private static class WindowHookFileReader extends DriverBackedFileReader {
        private final String mainFileName;
        private final String metaFileName;
        private Runnable hook = () -> {};
        private boolean armed = false;
        private boolean hasRun = false;

        WindowHookFileReader(String tableFileName) {
            super("");
            this.mainFileName = tableFileName;
            this.metaFileName = tableFileName + ".meta";
        }

        void setHook(Runnable hook) {
            this.hook = hook;
        }

        void arm() {
            this.armed = true;
            this.hasRun = false;
        }

        boolean hasRun() {
            return hasRun;
        }

        @Override
        public String move(String source, String dest) throws IOException {
            String result = super.move(source, dest);
            if (armed && (dest.endsWith("/" + mainFileName) || dest.endsWith("/" + metaFileName))) {
                armed = false;
                hook.run();
                hasRun = true;
            }
            return result;
        }
    }
}
