package org.gorpipe.s3.driver;

import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Class the either flushes or aborts S3ObjectInputStream up on close.
 */
class AbortingInputStream extends InputStream {
    private final long SKIP_BYTES_THRESHOLD = 1024L*1024L;
    private final S3ObjectInputStream s3is;
    private final long maxBytes;
    private long bytesRead = 0;

    public AbortingInputStream(S3ObjectInputStream s3is, GetObjectRequest request) {
        this.s3is = s3is;
        long[] range = request.getRange();
        long start = range == null || range.length < 1 ? 0 : range[0];
        long end = range == null || range.length < 2 ? Long.MAX_VALUE : range[range.length - 1];
        this.maxBytes = end - start + 1;
    }

    @Override
    public int read() throws IOException {
        bytesRead++;
        return s3is.read();
    }

    @Override
    public void close() throws IOException {
        if (bytesRead < maxBytes) {
            // We did not read all bytes.
            if (maxBytes - bytesRead < SKIP_BYTES_THRESHOLD) {
                // If we are close to the end, skip the rest (to avoid aborting)
                s3is.skip(maxBytes - bytesRead);
                s3is.close();
            } else {
                s3is.abort();
            }
        } else {
            s3is.close();
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        return s3is.read(b);
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        return s3is.read(b, off, len);
    }

    @Override
    public byte[] readAllBytes() throws IOException {
        return s3is.readAllBytes();
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        return s3is.readNBytes(len);
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        return s3is.readNBytes(b, off, len);
    }

    @Override
    public long skip(long n) throws IOException {
        return s3is.skip(n);
    }

    @Override
    public void skipNBytes(long n) throws IOException {
        s3is.skipNBytes(n);
    }

    @Override
    public int available() throws IOException {
        return s3is.available();
    }

    @Override
    public synchronized void mark(int readlimit) {
        s3is.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        s3is.reset();
    }

    @Override
    public boolean markSupported() {
        return s3is.markSupported();
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        return s3is.transferTo(out);
    }
}
