package org.gorpipe.base.streams;

import com.google.common.base.Preconditions;
import org.gorpipe.exceptions.GorResourceException;

import java.io.*;

public final class FullRetryInputStream extends FilterInputStream {

    public FullRetryInputStream(InputStream in) {
        super( Preconditions.checkNotNull(in));
    }

    @Override
    public int read() throws IOException {
        try {
        return super.read();
        } catch (Exception e) {
            throw new GorResourceException("Read error", "", e).fullRetry();
        }
    }

    @Override
    public int read(byte[] b) throws IOException {
        try {
            return super.read(b);
        } catch (Exception e) {
            throw new GorResourceException("Read buffer error", "", e).fullRetry();
        }
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            return super.read(b, off, len);
        } catch (Exception e) {
            throw new GorResourceException("Read offset error", "", e).fullRetry();
        }
    }

    @Override
    public long skip(long n) throws IOException {
        try {
        return super.skip(n);
        } catch (Exception e) {
            throw new GorResourceException("Skip error", "", e).fullRetry();
        }
    }

    @Override
    public synchronized void reset() throws IOException {
        try {
            super.reset();
        } catch (Exception e) {
            throw new GorResourceException("Reset error", "", e).fullRetry();
        }
    }

    public void close() {
        try {
            this.in.close();
        } catch (Exception e) {
            throw new GorResourceException("Close error", "", e).fullRetry();
        }
    }

}

