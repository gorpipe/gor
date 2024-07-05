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

import org.gorpipe.exceptions.GorException;
import org.gorpipe.gor.driver.adapters.PositionAwareInputStream;
import org.gorpipe.gor.driver.providers.stream.StreamUtils;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSourceMetadata;
import org.gorpipe.gor.driver.utils.RetryHandlerBase;

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

    private final RetryHandlerBase retry;

    public RetryStreamSourceWrapper(RetryHandlerBase retry, StreamSource wrapped) {
        super(wrapped);
        this.retry = retry;
    }

    @Override
    public InputStream open() {
        return retry.perform(() -> wrapStream(RetryStreamSourceWrapper.super.open(), 0, null));
    }

    @Override
    public InputStream open(long start) {
        return retry.perform(() -> wrapStream(RetryStreamSourceWrapper.super.open(start), start, null));
    }

    @Override
    public InputStream open(long start, long minLength) {
        return retry.perform(() -> wrapStream(RetryStreamSourceWrapper.super.open(start, minLength), start, minLength));
    }

    @Override
    public InputStream openClosable() {
        return retry.perform(() -> wrapStream(super.openClosable(), 0, null));
    }

    @Override
    public OutputStream getOutputStream(boolean append) {
        return retry.perform(() -> super.getOutputStream(append));
    }

    @Override
    public OutputStream getOutputStream(long position) {
        return retry.perform(() -> super.getOutputStream(position));
    }

    @Override
    public StreamSourceMetadata getSourceMetadata() {
        return retry.perform(super::getSourceMetadata);
    }

    @Override
    public boolean supportsWriting() {
        return super.supportsWriting();
    }

    @Override
    public boolean exists() {
        return retry.perform(super::exists);
    }

    @Override
    public void delete() {
         retry.perform(super::delete);
    }

    @Override
    public void deleteDirectory() {
        retry.perform(super::deleteDirectory);
    }

    @Override
    public boolean isDirectory() {
        return retry.perform(super::isDirectory);
    }

    @Override
    public String createDirectory(FileAttribute<?>... attrs) {
        return retry.perform(() -> super.createDirectory(attrs));
    }

    @Override
    public String createDirectoryIfNotExists(FileAttribute<?>... attrs) {
        return retry.perform(() -> super.createDirectoryIfNotExists(attrs));
    }

    @Override
    public String createDirectories(FileAttribute<?>... attrs) {
        return retry.perform(() -> super.createDirectories(attrs));
    }

    @Override
    public Stream<String> list() {
        return retry.perform(super::list);
    }

    @Override
    public Stream<String> walk() {
        return retry.perform(super::walk);
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
        public int read(byte[] b, int off, int len) {
            return retry.perform(() -> {
                            try {
                                return super.read(b, off, len);
                            } catch (GorException e) {
                                StreamUtils.tryClose(in);
                                in = reopen();
                                throw e;
                            }
                        });
        }

        /**
         * NB: If reopening the stream fails - it is not retried.
         */
        private InputStream reopen() {
            if (length == null) {
                return open(start + getPosition());
            } else {
                return open(start + getPosition(), length - getPosition());
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
