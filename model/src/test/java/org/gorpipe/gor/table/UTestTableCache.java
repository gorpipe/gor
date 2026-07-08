package org.gorpipe.gor.table;

import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.table.dictionary.gor.GorDictionaryCache;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;

public class UTestTableCache {

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    private Path writeSmallDict(String name) throws Exception {
        Path dir = workDir.getRoot().toPath();
        Files.writeString(dir.resolve("a.gor"), "chrom\tpos\n" + "chr1\t1\n");
        Path gord = dir.resolve(name);
        // one data line: file<TAB>alias
        Files.writeString(gord, "a.gor\tpn1\n");
        return gord;
    }

    @Test
    public void getTable_secondCallIsCacheHit_andStatsRecorded() throws Exception {
        Path gord = writeSmallDict("stats.gord");
        GorDictionaryCache cache = new GorDictionaryCache();
        DriverBackedFileReader reader = new DriverBackedFileReader("");

        long hitsBefore = cache.stats().hitCount();
        cache.getTable(gord.toString(), reader);   // miss -> put
        cache.getTable(gord.toString(), reader);   // hit
        Assert.assertEquals(1L, cache.estimatedSize());
        Assert.assertTrue("expected a cache hit to be recorded",
                cache.stats().hitCount() > hitsBefore);
    }

    @Test
    public void maxSizeSystemProperty_isHonored() throws Exception {
        String prev = System.getProperty("gor.dictionary.cache.maxsize");
        System.setProperty("gor.dictionary.cache.maxsize", "1");
        try {
            GorDictionaryCache cache = new GorDictionaryCache();
            DriverBackedFileReader reader = new DriverBackedFileReader("");
            cache.getTable(writeSmallDict("m1.gord").toString(), reader);
            cache.getTable(writeSmallDict("m2.gord").toString(), reader);
            cache.cleanUp();
            Assert.assertTrue("cache bounded to maxsize=1", cache.estimatedSize() <= 1L);
        } finally {
            if (prev == null) System.clearProperty("gor.dictionary.cache.maxsize");
            else System.setProperty("gor.dictionary.cache.maxsize", prev);
        }
    }
}
