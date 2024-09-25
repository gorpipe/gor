package org.gorpipe.base.streams;

import com.google.common.base.Preconditions;

import java.io.FilterOutputStream;
import java.io.OutputStream;
import java.io.IOException;

import org.gorpipe.exceptions.GorResourceException;

public final class FullRetryOutputStream extends FilterOutputStream {

    public FullRetryOutputStream(OutputStream out) {
        super( Preconditions.checkNotNull(out));
    }

    public void write(byte[] b, int off, int len) throws IOException {
        try {
            this.out.write(b, off, len);
        } catch (Exception e) {
            throw new GorResourceException("Write error", "", e).fullRetry();
        }
    }

    public void write(int b) throws IOException {
        try {
            this.out.write(b);
        } catch (Exception e) {
            throw new GorResourceException("Write error", "", e).fullRetry();
        }
    }

    public void close() {
        try {
            this.out.close();
        } catch (Exception e) {
            throw new GorResourceException("Close error", "", e).fullRetry();
        }
    }

}

