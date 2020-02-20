package org.gorpipe.model.genome.files.gor.pgen;

import org.junit.Assert;
import org.junit.Test;

import static org.gorpipe.model.genome.files.gor.pgen.PGenTestUtils.getRandomHardCalls;

public class UTestMainTrackWriter {

    @Test
    public void test_emptyArguments() {
        final MainTrackWriter mtw = new MainTrackWriter(new byte[0]);
        final byte[] buffer = new byte[0];
        final int bytesWritten = mtw.write(buffer, 0, 0);
        Assert.assertEquals(0, bytesWritten);
        Assert.assertTrue(mtw.done());
    }

    @Test
    public void test_callWriteWithEmptyBuffer() {
        final MainTrackWriter mtw = new MainTrackWriter(new byte[1]);
        final byte[] buffer = new byte[0];
        final int bytesWritten = mtw.write(buffer, 0, 0);
        Assert.assertEquals(0, bytesWritten);
        Assert.assertFalse(mtw.done());
    }

    @Test
    public void test_write_tooSmallBuffer() {
        final byte[] buffer = new byte[1];
        final byte[] hc = {0, 3, 2, 1, 2};
        final MainTrackWriter mtw = new MainTrackWriter(hc);

        int bytesWritten = mtw.write(buffer, 0, buffer.length);
        Assert.assertEquals(buffer.length, bytesWritten);
        Assert.assertEquals(0x6c,buffer[0] & 0xff);
        Assert.assertFalse(mtw.done());

        bytesWritten = mtw.write(buffer, 0, buffer.length);
        Assert.assertEquals(-buffer.length, bytesWritten);
        Assert.assertEquals(0x02, buffer[0] & 0xff);
        Assert.assertTrue(mtw.done());
    }

    @Test
    public void test_write_bigBuffer() {
        final byte[] buffer = new byte[2];
        for (int i = 0; i <= 8; ++i) {
            final byte[] hc = getRandomHardCalls(i);
            final MainTrackWriter mtw = new MainTrackWriter(hc);
            final int bytesWritten = mtw.write(buffer, 0, buffer.length);
            final int expected = -((i >> 2) + ((i & 3) == 0 ? 0 : 1));
            Assert.assertEquals(expected, bytesWritten);
            Assert.assertTrue(mtw.done());
        }
    }
}
