package org.gorpipe.s3.driver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Class the either flushes or aborts S3ObjectInputStream up on close.
 */
class AbortingInputStream extends InputStream {
    private static final Logger logger = LoggerFactory.getLogger(AbortingInputStream.class);

    private static final long SKIP_BYTES_THRESHOLD = 1024L*1024L;
    private final ResponseInputStream<GetObjectResponse> s3is;
    private final long maxBytes;
    private final AtomicLong bytesRead = new AtomicLong(0);

    public AbortingInputStream(ResponseInputStream<GetObjectResponse> s3is, GetObjectRequest request) {
        this.s3is = s3is;
        BytesRange range = new BytesRange(request.range());
        this.maxBytes = range.end() - range.start() + 1;
    }

    public long bytesRead() {
        return this.bytesRead.get();
    }

    @Override
    public int read() throws IOException {
        try {
            int read = s3is.read();
            this.updateBytesRead(read);
            return read;
        } catch (Exception e) {
            logger.warn("Exception reading from S3 stream", e);
            throw e;
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            int read = s3is.read(b, off, len);
            this.updateBytesRead(read);
            return read;
        } catch (Exception e) {
            logger.warn("Exception reading from S3 stream", e);
            throw e;
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        try {
            int read = s3is.read(b);
            this.updateBytesRead(read);
            return read;
        } catch (Exception e) {
            logger.warn("Exception reading from S3 stream", e);
            throw e;
        }
    }

    @Override
    public long skip(long n) throws IOException {
        try {
            long skipped = s3is.skip(n);
            this.updateBytesRead(skipped);
            return skipped;
        } catch (Exception e) {
            logger.warn("Exception skip from S3 stream", e);
            throw e;
        }
    }

    private void updateBytesRead(long read) {
        if (read > 0L) {
            this.bytesRead.addAndGet(read);
        }
    }

    @Override
    public void close() throws IOException {
        try {
            long missingBytes = maxBytes - bytesRead.get();
            if (missingBytes > 0) {
                // We did not read all bytes.
                if (missingBytes < SKIP_BYTES_THRESHOLD) {
                    // If we are close to the end, skip the rest (to avoid aborting)
                    long skipped = s3is.skip(missingBytes);
                    logger.debug("S3 stream, not all bytes read, skipping {} ({}) bytes out of {} bytes.", skipped, missingBytes, maxBytes);
                    s3is.close();
                } else {
                    logger.debug("S3 stream, not all bytes read, aborting S3 input stream. {} bytes not read, of {} bytes.", missingBytes, maxBytes);
                    s3is.abort();
                }
            } else {
                s3is.close();
            }
        } catch (Exception e) {
            logger.warn("Exception closing S3 stream", e);
            throw e;
        }

    }

    @Override
    public byte[] readAllBytes() throws IOException {
        try {
            return s3is.readAllBytes();
        } catch (Exception e) {
            logger.warn("Exception readAllBytes from S3 stream", e);
            throw e;
        }
    }

    @Override
    public byte[] readNBytes(int len) throws IOException {
        try {
            return s3is.readNBytes(len);
        } catch (Exception e) {
            logger.warn("Exception readNBytes from S3 stream", e);
            throw e;
        }
    }

    @Override
    public int readNBytes(byte[] b, int off, int len) throws IOException {
        try {
            return s3is.readNBytes(b, off, len);
        } catch (Exception e) {
            logger.warn("Exception readNBytes from S3 stream", e);
            throw e;
        }
    }

    @Override
    public void skipNBytes(long n) throws IOException {
        try {
            s3is.skipNBytes(n);
        } catch (Exception e) {
            logger.warn("Exception skipNbytes from S3 stream", e);
            throw e;
        }
    }

    @Override
    public int available() throws IOException {
        try {
            return s3is.available();
        } catch (Exception e) {
            logger.warn("Exception available from S3 stream", e);
            throw e;
        }
    }

    @Override
    public synchronized void mark(int readlimit) {
        try {
            s3is.mark(readlimit);
        } catch (Exception e) {
            logger.warn("Exception mark S3 stream", e);
            throw e;
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        try {
            s3is.reset();
        } catch (Exception e) {
            logger.warn("Exception reset S3 stream", e);
            throw e;
        }
    }

    @Override
    public boolean markSupported() {
        try {
            return s3is.markSupported();
        } catch (Exception e) {
            logger.warn("Exception markSupported from S3 stream", e);
            throw e;
        }
    }

    @Override
    public long transferTo(OutputStream out) throws IOException {
        try {
            return s3is.transferTo(out);
        } catch (Exception e) {
            logger.warn("Exception transferTo S3 stream", e);
            throw e;
        }
    }
}
