package org.gorpipe.gor.driver.providers.stream.sources.wrappers;

import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.UTestHttpSource;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by villi on 26/08/15.
 */
public class UTestFullRangeWrapper extends UTestHttpSource {

    @Override
    protected FullRangeWrapper createSource(String name) throws IOException {
        StreamSource toWrap = super.createSource(name);
        return new FullRangeWrapper(toWrap);
    }

    public long[] getSeeds() {
        long[] arr = {6578736100410L, System.nanoTime()};
        return arr;
    }

    @Test
    public void testSeekOutsideThreshold() throws IOException {
        FullRangeWrapper fs = new FullRangeWrapper(super.createSource(getDataName(lines10000File)), 10);
        byte[] buf = new byte[100];
        InputStream s1 = fs.open(0, 100);
        Assert.assertNull(fs.fullRangeStream);

        StreamUtils.readToBuffer(s1, buf, 0, 100);

        InputStream s2 = fs.open(111, 100);
        Assert.assertNotSame(s1, s2);
        Assert.assertNull(fs.fullRangeStream);
    }

    @Test
    public void testSeekWithinThreshold() throws IOException {
        FullRangeWrapper fs = new FullRangeWrapper(super.createSource(getDataName(lines10000File)), 10);
        byte[] buf = new byte[100];
        InputStream s1 = fs.open(0, 100);
        Assert.assertNull(fs.fullRangeStream);

        StreamUtils.readToBuffer(s1, buf, 0, 100);
        s1.close();
        InputStream s2 = fs.open(100, 100);
        Assert.assertNotSame(s1, s2);
        Assert.assertEquals(s2, fs.fullRangeStream);
        StreamUtils.readToBuffer(s2, buf, 0, 100);
        s2.close();

        InputStream s3 = fs.open(205, 100);
        Assert.assertEquals(s2, fs.fullRangeStream);
        Assert.assertEquals(s2, s3);
        StreamUtils.readToBuffer(s3, buf, 0, 100);
        s3.close();
        InputStream s4 = fs.open(320, 100);
        Assert.assertNotSame(s3, s4);
        Assert.assertNull(fs.fullRangeStream);
    }

}
