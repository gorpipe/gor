package org.gorpipe.gor.driver.providers.stream.sources.wrappers;

import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.UTestFileSource;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by villi on 26/08/15.
 */
public class UTestExtendedRangeWrapper extends UTestFileSource {

    @Override
    protected ExtendedRangeWrapper createSource(String name) {
        StreamSource toWrap = super.createSource(name);
        return new ExtendedRangeWrapper(toWrap, 10, 100);
    }

    public long[] getSeeds() {
        long[] arr = {System.nanoTime()};
        return arr;
    }

    @Test
    public void testSeekOutsideThreshold() throws IOException {
        ExtendedRangeWrapper fs = new ExtendedRangeWrapper(super.createSource(getDataName(lines10000File)), 10, 100);
        byte[] buf = new byte[100];
        InputStream s1 = fs.open(0, 100);
        StreamUtils.readToBuffer(s1, buf, 0, 100);
        s1.close();

        InputStream s2 = fs.open(111, 100);
        Assert.assertNotSame(s1, s2);
    }

    @Test
    public void testSeekWithinThreshold() throws IOException {
        ExtendedRangeWrapper fs = new ExtendedRangeWrapper(super.createSource(getDataName(lines10000File)), 10, 100);
        byte[] buf = new byte[100];
        InputStream s1 = fs.open(0, 100);

        StreamUtils.readToBuffer(s1, buf, 0, 100);
        s1.close();
        InputStream s2 = fs.open(100, 100);
        Assert.assertEquals(s2, fs.extendedRangeStream);
        StreamUtils.readToBuffer(s2, buf, 0, 100);
        s2.close();

        InputStream s3 = fs.open(205, 100);
        Assert.assertEquals(s2, fs.extendedRangeStream);
        Assert.assertEquals(s2, s3);
        StreamUtils.readToBuffer(s3, buf, 0, 100);
        s3.close();
        InputStream s4 = fs.open(320, 100);
        Assert.assertNotSame(s3, s4);
    }

}
