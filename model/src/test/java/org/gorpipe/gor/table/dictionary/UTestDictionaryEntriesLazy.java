package org.gorpipe.gor.table.dictionary;

import org.gorpipe.gor.table.dictionary.gor.GorDictionaryEntry;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryTable;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public class UTestDictionaryEntriesLazy {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private GorDictionaryTable buildTable() throws Exception {
        Path dir = workDir.getRoot().toPath();
        Files.writeString(dir.resolve("a.gor"), "chrom\tpos\nchr1\t1\n");
        Files.writeString(dir.resolve("b.gor"), "chrom\tpos\nchr1\t2\n");
        Path gord = dir.resolve("t.gord");
        Files.writeString(gord, "a.gor\tpn1\nb.gor\tpn2\n");
        return new GorDictionaryTable.Builder<>(gord.toString()).build();
    }

    // Reflect into the DictionaryEntries backing the table.
    private Object entriesOf(GorDictionaryTable table) throws Exception {
        Field f = DictionaryTableReader.class.getDeclaredField("tableEntries");
        f.setAccessible(true);
        return f.get(table);
    }

    private boolean boolField(Object entries, String name) throws Exception {
        Field f = DictionaryEntries.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.getBoolean(entries);
    }

    private Object refField(Object entries, String name) throws Exception {
        Field f = DictionaryEntries.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(entries);
    }

    @Test
    public void readPath_doesNotBuildContentMap() throws Exception {
        GorDictionaryTable table = buildTable();
        // Read path: filter by tag.
        List<GorDictionaryEntry> res = table.filter().tags("pn1").get();
        Assert.assertEquals(1, res.size());

        Object entries = entriesOf(table);
        Assert.assertFalse("content map must not be built by the read path",
                boolField(entries, "contentMapLoaded"));
        Assert.assertNull("contentHashToLines must be null after read-only path",
                refField(entries, "contentHashToLines"));
    }

    @Test
    public void getAllActiveTags_buildsContentMap_andReturnsTags() throws Exception {
        GorDictionaryTable table = buildTable();
        Object entries = entriesOf(table);

        @SuppressWarnings("unchecked")
        Set<String> tags = ((IDictionaryEntries<GorDictionaryEntry>) entries).getAllActiveTags();
        Assert.assertTrue(tags.contains("pn1"));
        Assert.assertTrue(tags.contains("pn2"));
        Assert.assertTrue("content map should be built once a consumer asks",
                boolField(entries, "contentMapLoaded"));
    }
}
