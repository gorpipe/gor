package org.gorpipe.base.streams;

import com.google.common.base.Preconditions;
import org.slf4j.Logger;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class LimitedOutputStream extends FilterOutputStream {
    Logger log = org.slf4j.LoggerFactory.getLogger(LimitedOutputStream.class);

    private long count;
    private long maxSize;

    public LimitedOutputStream(OutputStream out, long maxSize) {
        super((OutputStream) Preconditions.checkNotNull(out));
        this.maxSize = maxSize;
    }

    public long getCount() {
        return this.count;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            this.out.write(b, off, len);
            this.count += (long) len;
            this.check();
        } catch (Exception e) {
            log.warn("Exception in write buffer after writing {} bytes", this.count, e);
            throw e;
        }
    }

    @Override
    public void write(int b) throws IOException {
        try {
            this.out.write(b);
            ++this.count;
            this.check();
        } catch (Exception e) {
            log.warn("Exception in write byte after writing {} bytes", this.count, e);
            throw e;
        }
    }

    @Override
    public void flush() throws IOException {
        try {
            this.out.flush();
        } catch (Exception e) {
            log.warn("Exception in flush after writing {} bytes", this.count, e);
            throw e;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            this.out.close();
        } catch (Exception e) {
            log.warn("Exception in close after writing {} bytes", this.count, e);
            throw e;
        }
    }

    private void check() {
        if (this.count > this.maxSize) {
            throw new IllegalStateException("Output stream limit exceeded: " + this.count + " > " + this.maxSize);
        }
    }
}

