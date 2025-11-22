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

import org.gorpipe.base.config.converters.ByteSizeConverter;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.adapters.PersistentInputStream;
import org.gorpipe.gor.driver.providers.stream.RequestRange;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSourceMetadata;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by villi on 24/08/15.
 * <p>
 * Wrapper around a stream source.
 * <p>
 * If it detects consecutive range opens - it will extend the request size up to a maximum (default 100MB) .
 * Subsequent range requests will be served from the extended request stream as long as within a (forward) seekable range.
 * When boundaries are reached, new stream will be opened - again extending the request size if the maximum has not been reached.
 * <p>
 * Rationale:
 * When most remote/high latency stream sources get a request to open a stream specifying start position and length,
 * they will issue a ranged request to the remote source mirroring the requested position/length.
 * <p>
 * The Genomic Iterators typically use a buffer size of 64-128kbytes so each request will be of that size.
 * In general, there are two resaons for a requests: Seeks and sequential reads/scans.
 * <p>
 * For seeks - the iterators are taking a peek at certain location - normally seeking back and forth to search for a location.
 * In this case we want to minimize the request size.
 * <p>
 * For sequential reads, the iterators are iterating and reading through a larger range which will result in multiple
 * consecutive small requests which will suffer on connections with higher latency.
 * If we instead open a larger request behind the scenes and serve smaller requests from that, we get the benefit of TCP windowing and
 * streaming from the server to the client.
 * <p>
 * (NB - This is generally not a problem on low latency links (like local or lan filesystems - which in addition have buffering and read-ahead).
 */
public class ExtendedRangeWrapper extends WrappedStreamSource {
    private static final Logger log = LoggerFactory.getLogger(ExtendedRangeWrapper.class);

    public static final int DEFAULT_SEEK_THRESHOLD = ByteSizeConverter.parse(System.getProperty("org.gorpipe.gor.driver.extended_range_streaming.seek_threshold", "64 kb")).getBytesAsInt();
    public static final int DEFAULT_MIN_RANGE = ByteSizeConverter.parse(System.getProperty("org.gorpipe.gor.driver.extended_range_streaming.min_request_size", "128 kb")).getBytesAsInt();
    public static final int DEFAULT_MAX_RANGE = ByteSizeConverter.parse(System.getProperty("org.gorpipe.gor.driver.extended_range_streaming.max_request_size", "8 mb")).getBytesAsInt();

    private final int seekThreshold;
    private final int maxRange;

    ExtendedRangeStream extendedRangeStream;

    private StreamSourceMetadata sourceMeta;

    public ExtendedRangeWrapper(StreamSource source) {
        this(source, DEFAULT_SEEK_THRESHOLD, DEFAULT_MAX_RANGE);
    }

    public ExtendedRangeWrapper(StreamSource source, int seekThreshold, int maxRange) {
        super(source);
        this.seekThreshold = seekThreshold;
        this.maxRange = maxRange;
    }

    @Override
    public InputStream openClosable() {
        return open(0, DEFAULT_MIN_RANGE);
    }

    @Override
    public InputStream open() {
        return open(0, DEFAULT_MIN_RANGE);
    }

    @Override
    public InputStream open(long start, long minLength) {
        // If working with an extended range stream - see if it's seekable - then reuse, or close
        if (extendedRangeStream != null) {
            if (extendedRangeStream.isClosed()) {
                long streamPos = extendedRangeStream.realPosition();
                long seek = start - streamPos;
                if (seek >= 0 && seek <= seekThreshold) {
                    extendedRangeStream.reopen();
                    long seeked = 0;
                    if (seek > 0) {
                        do {
                            long skipcount = extendedRangeStream.skip(seek - seeked);
                            if (skipcount <= 0) break;
                            seeked += skipcount;
                        } while (seeked < seek);
                    }
                    if (seek == seeked) return extendedRangeStream;
                }
                log.debug("Seek is " + seek + " - close extended range stream");
                clearStream();
            } else {
                log.debug("Cannot reuse extendedRangeStream - not closed");
                // Best we can do is to forward request to source.
                return super.open(start, minLength);
            }
        }
        // Start a new stream.
        extendedRangeStream = new ExtendedRangeStream(super.open(start, minLength), RequestRange.fromFirstLength(start, minLength));
        return extendedRangeStream;
    }

    private void clearStream() {
        if (extendedRangeStream != null) {
            extendedRangeStream.reallyClose();
            extendedRangeStream = null;
        }
    }

    @Override
    public StreamSourceMetadata getSourceMetadata() {
        if (sourceMeta == null) {
            sourceMeta = super.getSourceMetadata();
        }
        return sourceMeta;
    }


    @Override
    public void close() {
        log.debug("{} closing - clearing streams", this);
        clearStream();
        super.close();
    }


    class ExtendedRangeStream extends PersistentInputStream {
        // Contains the last request made to the underlying source - i.e. reflects the 'in' stream
        RequestRange lastRequest;
        // Contains the range this stream object represents - original start position + accumulated length.
        RequestRange bookKeeping;

        public ExtendedRangeStream(InputStream stream, RequestRange range) {
            super(stream);
            this.lastRequest = this.bookKeeping = range;
        }

        @Override
        public int read(byte[] b, int off, int len) {
            try {
                // Check if read request is larger than remaining bytes in current 'in' stream
                if (len + getPosition() > bookKeeping.getLength()) {
                    // 1. Read remainder of stream and close.  As read is not guaranteed to read but 1 byte we want we do
                    //    repeated reads.   Some streams are unhappy if the whole stream is not read when we close.
                    int remainderLength = (int) (bookKeeping.getLength() - getPosition());
                    int read = 0;
                    while (read < remainderLength) {
                        int readNext = super.read(b, off + read, remainderLength - read);
                        if (readNext < 0) {
                            break;
                        }
                        read += readNext;
                    }
                    in.close();

                    // 2. Calculate request length - double of last request up to the maximum.  But no smaller than the remaining read.
                    //long rlen = Math.max(len - read, Math.min(lastRequest.getLength() * 2, maxRange));
                    long rlen = Math.max(len - read, Math.min(Math.max(lastRequest.getLength() * 2, DEFAULT_MIN_RANGE), maxRange));


                    // 3. Open new 'in' stream at last position + new request length
                    RequestRange range = RequestRange.fromFirstLength(bookKeeping.getFirst() + getPosition(), rlen);
                    this.in = ExtendedRangeWrapper.super.open(range.getFirst(), range.getLength());
                    this.lastRequest = range;
                    this.bookKeeping = RequestRange.fromFirstLast(bookKeeping.getFirst(), range.getLast());

                    // 4. Read the remainder of the data
                    int readNext = super.read(b, off + read, len - read);
                    if (readNext > 0) {
                        read += readNext;
                    }
                    if (read == 0) return -1;
                    return read;
                } else {
                    return super.read(b, off, len);
                }
            } catch (IOException e) {
                throw GorResourceException.fromIOException(e, getPath());
            }
        }

        @Override
        public long skip(long n) {
            int count = (int) n;
            if (count != n) {
                throw new RuntimeException("Skip should not be this large:" + n);
            }
            return read(new byte[count], 0, count);
        }

        public long realPosition() {
            return bookKeeping.getFirst() + getPosition();
        }
    }
}
