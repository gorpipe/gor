package org.gorpipe.base.streams;

import com.google.common.base.Preconditions;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public final class LimitedOutputStream extends FilterOutputStream {
    private long count;
    private long maxSize;

    public LimitedOutputStream(OutputStream out, long maxSize) {
        super((OutputStream) Preconditions.checkNotNull(out));
        this.maxSize = maxSize;
    }

    public long getCount() {
        return this.count;
    }

    public void write(byte[] b, int off, int len) throws IOException {
        this.out.write(b, off, len);
        this.count += (long)len;
        this.check();
    }

    public void write(int b) throws IOException {
        this.out.write(b);
        ++this.count;
        this.check();
    }

    public void close() throws IOException {
        this.out.close();
    }

    private void check() {
        if (this.count > this.maxSize) {
            throw new IllegalStateException("Output stream limit exceeded: " + this.count + " > " + this.maxSize);
        }
    }
}

