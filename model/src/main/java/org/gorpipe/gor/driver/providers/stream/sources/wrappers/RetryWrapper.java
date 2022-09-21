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

import org.gorpipe.gor.driver.adapters.PositionAwareInputStream;
import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSourceMetadata;
import org.gorpipe.gor.driver.utils.RetryHandler;

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
public class RetryWrapper extends WrappedStreamSource {
    private final int readRetries;
    private final int requestRetries;
    private final RetryHandler retry;

    public RetryWrapper(RetryHandler retry, StreamSource wrapped, int requestRetries, int readRetries) {
        super(wrapped);
        this.requestRetries = requestRetries;
        this.readRetries = readRetries;
        this.retry = retry;
    }

    @Override
    public InputStream open() throws IOException {
        return retry.tryOp(() -> wrapStream(RetryWrapper.super.open(), 0, null), requestRetries);
    }

    @Override
    public InputStream open(long start) throws IOException {
        return retry.tryOp(() -> wrapStream(super.open(start), start, null), requestRetries);
    }

    @Override
    public InputStream open(long start, long minLength) throws IOException {
        return retry.tryOp(() -> wrapStream(super.open(start, minLength), start, minLength), requestRetries);
    }

    @Override
    public InputStream openClosable() throws IOException {
        return retry.tryOp(() -> wrapStream(super.openClosable(), 0, null), requestRetries);
    }

    @Override
    public OutputStream getOutputStream(boolean append) throws IOException {
        return retry.tryOp(() -> super.getOutputStream(append), requestRetries);
    }

    @Override
    public OutputStream getOutputStream(long position) throws IOException {
        return retry.tryOp(() -> super.getOutputStream(position), requestRetries);
    }

    @Override
    public StreamSourceMetadata getSourceMetadata() throws IOException {
        return retry.tryOp(super::getSourceMetadata, requestRetries);
    }

    @Override
    public boolean supportsWriting() {
        return super.supportsWriting();
    }

    @Override
    public boolean exists() throws IOException {
        return retry.tryOp(super::exists, requestRetries);
    }

    @Override
    public boolean fileExists() throws IOException {
        return retry.tryOp(super::fileExists, requestRetries);
    }

    @Override
    public void delete() throws IOException {
         retry.tryOp(super::delete, requestRetries, null);
    }

    @Override
    public boolean isDirectory() {
        try {
            return retry.tryOp(super::isDirectory, requestRetries);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public String createDirectory(FileAttribute<?>... attrs) throws IOException {
        return retry.tryOp(() -> super.createDirectory(attrs), requestRetries);
    }

    @Override
    public String createDirectoryIfNotExists(FileAttribute<?>... attrs) throws IOException {
        return retry.tryOp(() -> super.createDirectoryIfNotExists(attrs), requestRetries);
    }

    @Override
    public String createDirectories(FileAttribute<?>... attrs) throws IOException {
        return retry.tryOp(() -> super.createDirectories(attrs), requestRetries);
    }

    @Override
    public Stream<String> list() throws IOException {
        return retry.tryOp(super::list, requestRetries);
    }

    private InputStream wrapStream(InputStream open, long start, Long length) {
        return new RetryInputStream(open, start, length);
    }

    /**
     * Wraps the opened stream - adding retry logic to read()
     */
    class RetryInputStream extends PositionAwareInputStream {
        // Holds the initial start/length values.
        private final long start;
        private final Long length;

        protected RetryInputStream(InputStream in, long start, Long length) {
            super(in);
            this.start = start;
            this.length = length;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return retry.tryOp(() -> super.read(b, off, len), readRetries, lastEx -> {
                StreamUtils.tryClose(in);
                in = reopen();
            });
        }

        /**
         * NB: If reopening the stream fails - it is not retried.
         */
        private InputStream reopen() throws IOException {
            if (length == null) {
                return RetryWrapper.super.open(start + getPosition());
            } else {
                return RetryWrapper.super.open(start + getPosition(), length - getPosition());
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
