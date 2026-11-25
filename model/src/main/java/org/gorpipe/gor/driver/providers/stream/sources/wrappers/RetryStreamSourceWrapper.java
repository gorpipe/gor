/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2019 WuXi NextCode Inc.
 *  All Rights Reserved.
 *
 *  GORpipe is free software: you can redistribute it and/or modify
 *  it under the terms of the AFFERO GNU General Public License as published by
 *  the Free Software Foundation.
 *
 *  GORpipe is distributed "AS-IS" AND WITHOUT ANY WARRANTY OF ANY KIND,
 *  INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 *  NON-INFRINGEMENT, OR FITNESS FOR A PARTICULAR PURPOSE. See
 *  the AFFERO GNU General Public License for the complete license terms.
 *
 *  You should have received a copy of the AFFERO GNU General Public License
 *  along with GORpipe.  If not, see <http://www.gnu.org/licenses/agpl-3.0.html>
 *
 *  END_COPYRIGHT
 */

package org.gorpipe.gor.driver.providers.stream.sources.wrappers;

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSourceChangedException;
import org.gorpipe.gor.driver.adapters.PositionAwareInputStream;
import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSourceMetadata;
import org.gorpipe.gor.driver.utils.RetryHandlerBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.attribute.FileAttribute;
import java.util.stream.Stream;

/**
 * Wraps a stream source so operations resulting in IOExceptions will be retried.
 * There are two levels/places where this can happen:
 * <p>
 * 1. In call to open().
 * This is easily retried by repeating the open() call on the underlying source
 * <p>
 * 2. During read on an opened stream.
 * In this case - we need to track the position in the underlying stream
 * On retry  - close/discard the underlying stream and reopen at the last successfully read position
 * <p>
 *
 * Note:
 * 1. The open calls do not use the defaultOnRetryOp (which skips retries for FileNotFound).  Is that because we
 *    want retries for open ()
 * Created by villi on 29/08/15.
 */
public class RetryStreamSourceWrapper extends WrappedStreamSource {

    private static final Logger log = LoggerFactory.getLogger(RetryStreamSourceWrapper.class);

    /** Set to false to resume a read into a replaced source (logged), as before ENGKNOW-4312. */
    public static final String FAIL_ON_SOURCE_CHANGED_PROPERTY = "gor.retry.fail.on.source.changed";

    private final RetryHandlerBase retry;

    public RetryStreamSourceWrapper(RetryHandlerBase retry, StreamSource wrapped) {
        super(wrapped);
        this.retry = retry;
    }

    @Override
    public InputStream open() {
        return retry.perform("open", () -> wrapStream(super.open(), 0, null, false));
    }

    @Override
    public InputStream open(long start) {
        return retry.perform("open", () -> wrapStream(super.open(start), start, null, false));
    }

    @Override
    public InputStream open(long start, long minLength) {
        return retry.perform("open", () -> wrapStream(super.open(start, minLength), start, minLength, false));
    }

    @Override
    public InputStream openClosable() {
        return retry.perform("open", () -> wrapStream(super.openClosable(), 0, null, true));
    }

    @Override
    public OutputStream getOutputStream(boolean append) {
        return retry.perform("write", () -> super.getOutputStream(append));
    }

    @Override
    public OutputStream getOutputStream(long position) {
        return retry.perform("write", () -> super.getOutputStream(position));
    }

    @Override
    public StreamSourceMetadata getSourceMetadata() {
        return retry.perform("metadata", super::getSourceMetadata);
    }

    @Override
    public boolean supportsWriting() {
        return super.supportsWriting();
    }

    @Override
    public boolean exists() {
        return retry.perform("exists", super::exists);
    }

    @Override
    public void delete() {
         retry.perform("delete", super::delete);
    }

    @Override
    public void deleteDirectory() {
        retry.perform("delete", super::deleteDirectory);
    }

    @Override
    public boolean isDirectory() {
        return retry.perform("isdir", super::isDirectory);
    }

    @Override
    public String createDirectory(FileAttribute<?>... attrs) {
        return retry.perform("mkdir", () -> super.createDirectory(attrs));
    }

    @Override
    public String createDirectoryIfNotExists(FileAttribute<?>... attrs) {
        return retry.perform("mkdir", () -> super.createDirectoryIfNotExists(attrs));
    }

    @Override
    public String createDirectories(FileAttribute<?>... attrs) {
        return retry.perform("mkdir", () -> super.createDirectories(attrs));
    }

    @Override
    public Stream<String> list() {
        return retry.perform("list", super::list);
    }

    @Override
    public Stream<String> walk() {
        return retry.perform("walk", super::walk);
    }

    private InputStream wrapStream(InputStream open, long start, Long length, boolean closesSource) {
        return new RetryInputStream(open, start, length, closesSource);
    }

    /**
     * Wraps the opened stream - adding retry logic to read()
     */
    class RetryInputStream extends PositionAwareInputStream {
        // Holds the initial start/length values.
        private final long start;
        private final Long length;
        // Identity of what the stream was opened on (null: not tracked by the source).
        private Object openIdentity;
        private IOException lastFailure;
        // Set when a reopen found the source replaced; every later read fails with it.
        private GorSourceChangedException changed;
        // Opened with openClosable(): closing the stream closes the source. A reopen swaps in a stream that doesn't,
        // so close() then closes the source itself.
        private final boolean closesSource;
        private boolean reopened;

        protected RetryInputStream(InputStream in, long start, Long length, boolean closesSource) {
            super(in);
            this.start = start;
            this.length = length;
            this.closesSource = closesSource;
            this.openIdentity = RetryStreamSourceWrapper.super.getReadIdentity();
        }

        @Override
        public int read() throws IOException {
            // Through the retrying read, so single-byte reads are retried and checked too.
            byte[] b = new byte[1];
            int n = read(b, 0, 1);
            return n <= 0 ? -1 : b[0] & 0xff;
        }

        @Override
        public int available() throws IOException {
            checkNotChanged();
            return super.available();
        }

        @Override
        public void close() throws IOException {
            try {
                super.close();
            } finally {
                if (closesSource && reopened) {
                    RetryStreamSourceWrapper.super.close();
                }
            }
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            checkNotChanged();
            return retry.perform("read", () -> {
                            try {
                                return super.read(b, off, len);
                            } catch (IOException e) {
                                lastFailure = e;
                                throw GorResourceException.fromIOException(e, getPath()).retry();
                            }
                        }, this::reopen);
        }

        @Override
        public long skip(long n) throws IOException {
            checkNotChanged();
            return retry.perform("skip", () -> {
                try {
                    return super.skip(n);
                } catch (IOException e) {
                    lastFailure = e;
                    throw GorResourceException.fromIOException(e, getPath()).retry();
                }
            }, this::reopen);
        }

        /**
         * NB: If reopening the stream fails - it is not retried.
         * <p>
         * Resuming at the offset reached is only correct when the path still names the object the stream was opened
         * on. If it was replaced (ESTALE on NFS after a move over the file), the read fails with
         * {@link GorSourceChangedException} instead of mixing the old prefix with the new content (ENGKNOW-4312).
         */
        private void reopen() {
            // Close the current stream
            StreamUtils.tryClose(in);
            // Close the underlying source stream (to clear any cached state)
            RetryStreamSourceWrapper.super.close();
            reopened = true;

            long offset = start + getPosition();
            // Need to open it using the outer super class open (and be careful NOT to wrap it again)
            if (length == null) {
                in = RetryStreamSourceWrapper.super.open(offset);
            } else {
                in = RetryStreamSourceWrapper.super.open(offset, length - getPosition());
            }

            Object current = RetryStreamSourceWrapper.super.getReadIdentity();
            if (offset > 0 && openIdentity != null && !openIdentity.equals(current)) {
                String reason = current == null ? "identity unavailable after reopen" : "replaced";
                if (Boolean.parseBoolean(System.getProperty(FAIL_ON_SOURCE_CHANGED_PROPERTY, "true"))) {
                    log.warn("Source changed during read: {} offset={} ({}), not resuming", getPath(), offset, reason);
                    // Leave the stream unusable: reading on would give the new content at the old offset.
                    StreamUtils.tryClose(in);
                    RetryStreamSourceWrapper.super.close();
                    changed = new GorSourceChangedException(String.valueOf(getPath()), offset, lastFailure);
                    throw changed;
                }
                log.warn("Source changed during read: {} offset={} ({}), resuming ({}=false)", getPath(), offset,
                        reason, FAIL_ON_SOURCE_CHANGED_PROPERTY);
            }
            // Everything read from here on comes from what was just opened.
            openIdentity = current;
        }

        private void checkNotChanged() {
            if (changed != null) {
                throw changed;
            }
        }

        /**
         * Supporting mark/reset is complicated - because the mark might have been set on the previousloy wrapped stream that is now closed.
         * We can add that later if really needed but a simpler method would be to wrap with BufferedInputStream (although possibly slower).
         */
        @Override
        public boolean markSupported() {
            return false;
        }

    }

}
