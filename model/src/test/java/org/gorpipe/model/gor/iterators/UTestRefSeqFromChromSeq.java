package org.gorpipe.model.gor.iterators;

import org.gorpipe.gor.model.DriverBackedFileReader;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;

public class UTestRefSeqFromChromSeq {

    @Rule
    public final ProvideSystemProperty cacheFolder
            = new ProvideSystemProperty("gor.refseq.cache.folder", "/tmp/cache");

    @Rule
    public final ProvideSystemProperty triggerDownload
            = new ProvideSystemProperty("gor.refseq.cache.download", "False");

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();

    @Rule
    public final RestoreSystemProperties restoreSystemProperties = new RestoreSystemProperties();

    @Rule
    public final SystemErrRule systemErrRule = new SystemErrRule().enableLog();



    @Test
    public void testGetRefbase() {

        RefSeqFromChromSeq refseq = new RefSeqFromChromSeq("../tests/data/ref_mini/chromSeq", new DriverBackedFileReader(""));

        Assert.assertEquals('C', refseq.getBase("chr1", 101000));

        // OutSide, within existing buffer.
        Assert.assertEquals( 'N', refseq.getBase("chr1", 249255));

        // OutSide from different buffers.
        Assert.assertEquals( 'N', refseq.getBase("chr1", 250000));

        // Outside from same buffer, with fresh refseq
        refseq = new RefSeqFromChromSeq("../tests/data/ref_mini/chromSeq", new DriverBackedFileReader(""));
        Assert.assertEquals( 'N', refseq.getBase("chr1", 250001));
    }


    @Test
    public void testGetRefbases() {

        RefSeqFromChromSeq refseq = new RefSeqFromChromSeq("../tests/data/ref_mini/chromSeq", new DriverBackedFileReader(""));

        Assert.assertEquals("C", refseq.getBases("chr1", 101000, 101000));

        Assert.assertEquals("CAG", refseq.getBases("chr1", 101000, 101002));

        Assert.assertEquals(10003, refseq.getBases("chr1", 101000, 111002).length());

        // OutSide, within existing buffer.
        Assert.assertEquals( "NN", refseq.getBases("chr1", 249255, 249256));

        // OutSide from different buffers.
        Assert.assertEquals( "NN", refseq.getBases("chr1", 250000, 250001));

        // Outside from same buffer.
        Assert.assertEquals( "NN", refseq.getBases("chr1", 250001, 250002));

        // Outside from same buffer, with fresh refseq
        refseq = new RefSeqFromChromSeq("../tests/data/ref_mini/chromSeq", new DriverBackedFileReader(""));
        Assert.assertEquals( "NN", refseq.getBases("chr1", 250001, 250002));

    }

    @Ignore("Run manually to test from same buffer optimization")
    @Test
    public void testGetRefbasesPerformance() {
        long startTime;
        RefSeqFromChromSeq refseq = new RefSeqFromChromSeq("../tests/data/ref_mini/chromSeq", new DriverBackedFileReader(""));

        // Prep buffers.
        refseq.getBase("chr1", 100001);
        refseq.getBase("chr1", 110001);

        // Different buffers.
        startTime = System.nanoTime();
        refseq.getBases("chr1", 105001, 114999);
        long diffBuffer = System.nanoTime() - startTime;

        // Within the same buffer.
        startTime = System.nanoTime();
        refseq.getBases("chr1", 100001, 109999);
        long sameBuffer = System.nanoTime() - startTime;

        System.out.println(String.format("Same buffer: %d, diff buffers: %d", sameBuffer, diffBuffer));
    }

    // Test getFullCachePath
    @Test
    public void testGetFullCachePath() {
        var refPath = "../tests/data/ref_mini/chromSeq";
        var fullRefPath = Path.of(refPath).toAbsolutePath();
        RefSeqFromChromSeq refseq = new RefSeqFromChromSeq(refPath, new DriverBackedFileReader(""));
        Assert.assertEquals("/tmp/cache/ref_mini/chromSeq", refseq.getFullCachePath(fullRefPath).toString());
    }

    @Test
    public void testGetRefbaseFromCache() throws InterruptedException {

        Path workDirPath = workDir.getRoot().toPath();

        System.setProperty("gor.refseq.cache.download", "True");
        System.setProperty("gor.refseq.cache.folder", workDirPath.resolve("cache").toString());

        RefSeqFromChromSeq refseq = new RefSeqFromChromSeq("../tests/data/ref_mini/chromSeq", new DriverBackedFileReader(""));

        RefSeqFromChromSeq.downloadTriggered().clear();

        Assert.assertEquals('C', refseq.getBase("chr1", 101000));

        // Wait for download to finish.
        long startWaitTime = System.currentTimeMillis();
        while (!Files.exists(workDirPath.resolve("cache").resolve("ref_mini").resolve("chromSeq"))) {
            Thread.sleep(50);
            if (System.currentTimeMillis() - startWaitTime > 2000) {
                throw new RuntimeException("Timeout waiting for download to finish");
            }
        }

        Assert.assertEquals('C', refseq.getBase("chr1", 101000));
    }
}
