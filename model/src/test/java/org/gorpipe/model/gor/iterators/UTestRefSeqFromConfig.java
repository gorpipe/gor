package org.gorpipe.model.gor.iterators;

import org.gorpipe.gor.model.DriverBackedFileReader;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class UTestRefSeqFromConfig {

    @Test
    public void testGetRefbase() {

        RefSeqFromConfig refseq = new RefSeqFromConfig("../tests/data/ref_mini/chromSeq", new DriverBackedFileReader(""));

        Assert.assertEquals('C', refseq.getBase("chr1", 101000));

        // OutSide, within existing buffer.
        Assert.assertEquals( 'N', refseq.getBase("chr1", 249255));

        // OutSide from different buffers.
        Assert.assertEquals( 'N', refseq.getBase("chr1", 250000));

        // Outside from same buffer, with fresh refseq
        refseq = new RefSeqFromConfig("../tests/data/ref_mini/chromSeq", new DriverBackedFileReader(""));
        Assert.assertEquals( 'N', refseq.getBase("chr1", 250001));
    }


    @Test
    public void testGetRefbases() {

        RefSeqFromConfig refseq = new RefSeqFromConfig("../tests/data/ref_mini/chromSeq", new DriverBackedFileReader(""));

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
        refseq = new RefSeqFromConfig("../tests/data/ref_mini/chromSeq", new DriverBackedFileReader(""));
        Assert.assertEquals( "NN", refseq.getBases("chr1", 250001, 250002));

    }

    @Ignore("Run manually to test from same buffer optimization")
    @Test
    public void testGetRefbasesPerformance() {
        long startTime;
        RefSeqFromConfig refseq = new RefSeqFromConfig("../tests/data/ref_mini/chromSeq", new DriverBackedFileReader(""));

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


}
