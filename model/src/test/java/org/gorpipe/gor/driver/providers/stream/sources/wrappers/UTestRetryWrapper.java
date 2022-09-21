package org.gorpipe.gor.driver.providers.stream.sources.wrappers;

import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.gor.driver.providers.stream.sources.DummyStreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.UTestFileSource;
import org.gorpipe.gor.driver.utils.RetryHandler;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by villi on 26/08/15.
 */
public class UTestRetryWrapper extends UTestFileSource {


    private RetryHandler handler;

    @Before
    public void setUp() {
        handler = new RetryHandler(0, 6400, 2, null);
    }

    @Override
    protected RetryWrapper createSource(String name) {
        StreamSource toWrap = super.createSource(name);
        return new RetryWrapper(handler, toWrap, 3, 4);
    }

    public long[] getSeeds() {
        long[] arr = {System.nanoTime()};
        return arr;
    }

    @Test
    public void testRecoverableOpen() throws IOException {
        RetryWrapper fs = new RetryWrapper(handler, new FailingStreamSource("ABCD", 3, 0), 3, 0);
        Assert.assertNotNull(fs.open());
    }

    @Test
    public void testUnRecoverableOpen() {
        RetryWrapper fs = new RetryWrapper(handler, new FailingStreamSource("ABCD", 3, 0), 2, 0);
        try {
            fs.open();
            Assert.fail("Should not recover");
        } catch (IOException e) {
            // Pass
        }
    }

    @Test
    public void testRecoverableRead() throws IOException {
        RetryWrapper fs = new RetryWrapper(handler, new FailingStreamSource("ABCD", 0, 3), 0, 3);
        InputStream s = fs.open();
        Assert.assertEquals("ABCD", StreamUtils.readString(s, 100));
    }

    @Test
    public void testRecoverableFragmentedRead() throws IOException {
        String data = "asdkfjweihækv124346456234";
        RetryWrapper fs = new RetryWrapper(handler, new FailingStreamSource(data, 0, 3), 0, 3);
        InputStream s = fs.open();

        byte[] buf = new byte[100];

        int read;
        int total = 0;
        do {
            read = StreamUtils.readToBuffer(s, buf, total, 5);
            if (read > 0) total += read;
        } while (read > 0);

        Assert.assertEquals(data, new String(buf, 0, total));
    }

    @Test
    public void testUnRecoverableRead() throws IOException {
        RetryWrapper fs = new RetryWrapper(handler, new FailingStreamSource("ABCD", 0, 3), 0, 2);
        InputStream s = fs.open();
        try {
            StreamUtils.readString(s, 100);
            Assert.fail("Should fail during read");
        } catch (IOException e) {
            // Pass
        }
    }

    /**
     * Implements a dummy stream source that will fail on open as well as each read.
     */
    class FailingStreamSource extends DummyStreamSource {
        private byte[] data;
        private int readFails;
        private final int openFails;
        int openFailed = 0;
        int readFailed = 0;

        public FailingStreamSource(String data, int openFails, int readFails) {
            this.data = data.getBytes();
            this.openFails = openFails;
            this.readFails = readFails;
        }

        @Override
        public InputStream open() throws IOException {
            return open(0, data.length);
        }

        @Override
        public InputStream open(long start) throws IOException {
            return open(start, data.length - start);
        }

        @Override
        public InputStream open(long start, long length) throws IOException {
            if (openFailed < openFails) {
                openFailed++;
                throw new IOException("Failing " + openFailed);
            }
            return new FailingStream(new ByteArrayInputStream(data, (int) start, (int) length));
        }

        class FailingStream extends FilterInputStream {

            protected FailingStream(InputStream in) {
                super(in);
            }

            @Override
            public int read() {
                throw new UnsupportedOperationException("Not supported");
            }

            @Override
            public int read(byte[] b, int off, int len) throws IOException {
                if (readFailed < readFails) {
                    readFailed++;
                    throw new IOException("Read failing " + readFailed);
                }
                readFailed = 0;
                return super.read(b, off, len);
            }
        }
    }

}
