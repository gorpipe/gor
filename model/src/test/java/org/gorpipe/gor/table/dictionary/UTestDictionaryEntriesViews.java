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

/**
 * Deterministic regression tests for the read-accessor hardening in {@link DictionaryEntries}:
 * <ul>
 *   <li>{@code getEntries()} and {@code getAllActiveTags()} must return unmodifiable views so
 *       callers cannot structurally modify the shared backing state (finding #3).</li>
 *   <li>The internal mutators ({@code insert}/{@code delete}) must keep working after being
 *       switched off the public {@code getEntries()} accessor (regression for the refactor).</li>
 *   <li>{@code getEntries(String...)} must lazily reload after {@code clear()} without tripping
 *       over a null backing list (finding #2 guard).</li>
 * </ul>
 */
public class UTestDictionaryEntriesViews {

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

    @SuppressWarnings("unchecked")
    private IDictionaryEntries<GorDictionaryEntry> entriesOf(GorDictionaryTable table) throws Exception {
        Field f = DictionaryTableReader.class.getDeclaredField("tableEntries");
        f.setAccessible(true);
        return (IDictionaryEntries<GorDictionaryEntry>) f.get(table);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getEntries_returnsUnmodifiableList() throws Exception {
        IDictionaryEntries<GorDictionaryEntry> entries = entriesOf(buildTable());
        List<GorDictionaryEntry> list = entries.getEntries();
        Assert.assertEquals(2, list.size());
        list.clear(); // must throw - callers must not mutate the shared backing list
    }

    @Test(expected = UnsupportedOperationException.class)
    public void getAllActiveTags_returnsUnmodifiableSet() throws Exception {
        IDictionaryEntries<GorDictionaryEntry> entries = entriesOf(buildTable());
        Set<String> tags = entries.getAllActiveTags();
        Assert.assertTrue(tags.contains("pn1"));
        // Guava's Multiset.elementSet() blocks add() but SUPPORTS remove()/clear(), which would
        // strip occurrences from the internal multiset. The returned view must reject removal too.
        tags.clear(); // must throw - live view of internal multiset must not be mutable
    }

    @Test
    public void insertAndDelete_stillMutateBackingList() throws Exception {
        GorDictionaryTable table = buildTable();
        IDictionaryEntries<GorDictionaryEntry> entries = entriesOf(table);

        Assert.assertEquals(2, entries.size());

        GorDictionaryEntry newEntry =
                (GorDictionaryEntry) new GorDictionaryEntry.Builder("c.gor", table.getRootPath()).alias("pn3").build();
        entries.insert(newEntry, false);
        Assert.assertEquals(3, entries.size());

        entries.delete(newEntry, false);
        Assert.assertEquals(2, entries.size());
    }

    @Test
    public void getEntriesByTag_reloadsAfterClear() throws Exception {
        GorDictionaryTable table = buildTable();
        IDictionaryEntries<GorDictionaryEntry> entries = entriesOf(table);

        Assert.assertEquals(1, entries.getEntries("pn1").size());

        entries.clear();

        // After clear the backing list is dropped; the tag-filtered read must reload it,
        // not dereference a null rawLines (finding #2).
        List<GorDictionaryEntry> afterClear = entries.getEntries("pn1");
        Assert.assertEquals(1, afterClear.size());
    }
}
