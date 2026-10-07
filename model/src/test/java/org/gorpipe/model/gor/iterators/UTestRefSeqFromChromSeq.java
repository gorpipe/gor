package org.gorpipe.model.gor.iterators;

import org.gorpipe.gor.model.DriverBackedFileReader;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.RacFile;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.junit.contrib.java.lang.system.SystemErrRule;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.IOException;
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

    @Test
    public void testGetRefbasesMissingContigAfterOtherContig() {
        RefSeqFromChromSeq refseq = new RefSeqFromChromSeq("../tests/data/ref_mini/chromSeq", new DriverBackedFileReader(""));

        // Warm the buffer with a contig that has a sequence.
        Assert.assertEquals("CAG", refseq.getBases("chr1", 101000, 101002));

        // Contig without a sequence file must not return bases from the previous contig's buffer.
        Assert.assertEquals("NNN", refseq.getBases("chrXY", 101000, 101002));
        Assert.assertEquals('N', refseq.getBase("chrXY", 101000));

        // Switching back still works.
        Assert.assertEquals("CAG", refseq.getBases("chr1", 101000, 101002));
    }

    @Test
    public void testGetRefbasesMissingContigFirst() {
        RefSeqFromChromSeq refseq = new RefSeqFromChromSeq("../tests/data/ref_mini/chromSeq", new DriverBackedFileReader(""));

        // First read on a contig without a sequence file must not fail.
        Assert.assertEquals("NN", refseq.getBases("chrXY", 10, 11));
        Assert.assertEquals("CAG", refseq.getBases("chr1", 101000, 101002));
    }

    @Test
    public void testGetRefbasesMissingContigAcrossBuffers() {
        RefSeqFromChromSeq refseq = new RefSeqFromChromSeq("../tests/data/ref_mini/chromSeq", new DriverBackedFileReader(""));

        Assert.assertEquals("CAG", refseq.getBases("chr1", 101000, 101002));
        Assert.assertEquals("N".repeat(10006), refseq.getBases("chrXY", 9998, 20003));
    }

    @Test
    public void testGetRefbasesFailedReadIsTriedOncePerBuffer() throws IOException {
        // A mocked FileReader has no absolute paths for the cache folder lookup.
        System.clearProperty("gor.refseq.cache.folder");

        RacFile failingFile = Mockito.mock(RacFile.class);
        Mockito.when(failingFile.read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt()))
                .thenThrow(new RuntimeException("simulated read failure"));
        FileReader fileReader = Mockito.mock(FileReader.class);
        Mockito.when(fileReader.openFile(Mockito.anyString())).thenReturn(failingFile);

        RefSeqFromChromSeq refseq = new RefSeqFromChromSeq("refbuild", fileReader);

        // Range within one buffer: one read attempt, not one per position.
        Assert.assertEquals("NNN", refseq.getBases("chr1", 101000, 101002));
        Mockito.verify(failingFile, Mockito.times(1)).read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt());

        // Range across a buffer boundary: one read attempt per buffer.
        Assert.assertEquals("NNN", refseq.getBases("chr1", 109999, 110001));
        Mockito.verify(failingFile, Mockito.times(3)).read(Mockito.any(byte[].class), Mockito.anyInt(), Mockito.anyInt());

        Assert.assertEquals('N', refseq.getBase("chr1", 101000));
    }

    // Positions below 1 (e.g. pos 0 from an unmapped liftover row) read as 'N', like positions past the chromosome end.
    @Test
    public void testGetRefbasesBelowPositionOne() {
        String path = "../tests/data/ref_mini/chromSeq";

        // chr17 in ref_mini starts with real bases (AAGC...).
        RefSeqFromChromSeq refseq = new RefSeqFromChromSeq(path, new DriverBackedFileReader(""));
        Assert.assertEquals("NAAGC", refseq.getBases("chr17", 0, 4));

        // Same, with the buffer already loaded.
        Assert.assertEquals("NAAGC", refseq.getBases("chr17", 0, 4));

        // Fresh refseq, no buffer loaded yet.
        refseq = new RefSeqFromChromSeq(path, new DriverBackedFileReader(""));
        Assert.assertEquals("NNNNN", refseq.getBases("chr17", -5, -1));

        Assert.assertEquals("N", refseq.getBases("chr17", 0, 0));
        Assert.assertEquals('N', refseq.getBase("chr17", 0));
        Assert.assertEquals('N', refseq.getBase("chr17", -1));
        Assert.assertEquals('N', refseq.getBase("chr17", -10001));

        // Starting below 1 and crossing the buffer boundary (10000).
        String expectedTail = new RefSeqFromChromSeq(path, new DriverBackedFileReader("")).getBases("chr17", 1, 10002);
        refseq = new RefSeqFromChromSeq(path, new DriverBackedFileReader(""));
        String bases = refseq.getBases("chr17", -2, 10002);
        Assert.assertEquals(10005, bases.length());
        Assert.assertEquals("NNN" + expectedTail, bases);
        Assert.assertTrue(bases.startsWith("NNNAAGC"));
        Assert.assertTrue(bases.endsWith("aactctt"));

        // Starting more than a buffer below 1.
        refseq = new RefSeqFromChromSeq(path, new DriverBackedFileReader(""));
        Assert.assertEquals("N".repeat(10006) + "AA", refseq.getBases("chr17", -10005, 2));

        // Positive positions are unchanged.
        Assert.assertEquals("AAGC", refseq.getBases("chr17", 1, 4));
        Assert.assertEquals('A', refseq.getBase("chr17", 1));
        Assert.assertEquals("aactcttgac", refseq.getBases("chr17", 9996, 10005));
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
