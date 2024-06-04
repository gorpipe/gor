package org.gorpipe.gor.driver.providers.stream.sources.wrappers;

import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.gor.driver.providers.stream.sources.DummyStreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.UTestFileSource;
import org.gorpipe.gor.driver.utils.RetryHandlerBase;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by villi on 26/08/15.
 */
public class UTestRetryWrapper extends UTestFileSource {

    private static RetryHandlerBase createHandler(int retries) {
        return new TestRetryHandlerWithFixedRetries(100, 6400, 2, retries);
    }

    @Override
    protected RetryStreamSourceWrapper createSource(String name) {
        StreamSource toWrap = super.createSource(name);
        return new RetryStreamSourceWrapper(createHandler(3), toWrap);
    }

    @Test
    public void testRecoverableOpen() throws IOException {
        RetryStreamSourceWrapper fs = new RetryStreamSourceWrapper(createHandler(3), new FailingStreamSource("ABCD", 3, 0));
        Assert.assertNotNull(fs.open());
    }

    @Test
    public void testUnRecoverableOpen() {
        RetryStreamSourceWrapper fs = new RetryStreamSourceWrapper(createHandler(2), new FailingStreamSource("ABCD", 3, 0));
        Assert.assertThrows(GorSystemException.class, fs::open);
    }

    @Test
    public void testRecoverableRead() throws IOException {
        RetryStreamSourceWrapper fs = new RetryStreamSourceWrapper(createHandler(4), new FailingStreamSource("ABCD", 0, 3));
        InputStream s = fs.open();
        Assert.assertEquals("ABCD", StreamUtils.readString(s, 100));
    }

    @Test
    public void testRecoverableFragmentedRead() throws IOException {
        String data = "asdkfjweihækv124346456234";
        RetryStreamSourceWrapper fs = new RetryStreamSourceWrapper(createHandler(4), new FailingStreamSource(data, 0, 3));
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
    public void testUnRecoverableRead() {
        RetryStreamSourceWrapper fs = new RetryStreamSourceWrapper(createHandler(0), new FailingStreamSource("ABCD", 0, 3));
        InputStream s = fs.open();
        try {
            StreamUtils.readString(s, 100);
            Assert.fail("Should fail during read");
        } catch (IOException | GorException e) {
            // Pass
        }
    }

    /**
     * Implements a dummy stream source that will fail on open as well as each read.
     */
    static class FailingStreamSource extends DummyStreamSource {
        private final byte[] data;
        private final int readFails;
        private final int openFails;
        int openFailed = 0;
        int readFailed = 0;

        public FailingStreamSource(String data, int openFails, int readFails) {
            this.data = data.getBytes();
            this.openFails = openFails;
            this.readFails = readFails;
        }

        @Override
        public InputStream open() {
            return open(0, data.length);
        }

        @Override
        public InputStream open(long start) {
            return open(start, data.length - start);
        }

        @Override
        public InputStream open(long start, long length) {
            if (openFailed < openFails) {
                openFailed++;
                throw new GorResourceException("Failing " + openFailed, "").retry();
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
            public int read(byte[] b, int off, int len) {
                if (readFailed < readFails) {
                    readFailed++;
                    throw new GorResourceException("Read failing " + readFailed, "").retry();
                }
                readFailed = 0;
                try {
                    return super.read(b, off, len);
                } catch (IOException e) {
                    throw new GorResourceException("Failed to read", "", e).retry();
                }
            }
        }
    }

}
