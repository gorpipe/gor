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
 * If it detects consecutive range opens - it opens a full ranged stream
 * on the underlying datasource. Subsequent range requests will be served from the full range
 * stream as long as within a (forward) seekable range.
 * <p>
 * Rationale:
 * When most remote/high latency stream sources get a request to open a stream specifying start position and length,
 * they will issue a ranged request to the remote source mirroring the requested position/length.
 * <p>
 * The Genomic Iterators typically use a buffer size of 64-128kbytes so each request will be of that size.
 * In general, there are two reaons for a requests: Seeks and sequential reads/scans.
 * <p>
 * For seeks - the iterators are taking a peek at certain location - normally seeking back and forth to search for a location.
 * In this case we want to minimize the request size.
 * <p>
 * For sequential reads, the iterators are iterating and reading through a larger range which will result in multiple
 * consecutive small requests which will suffer on connections with higher latency.
 * If we instead open a full range request behind the scenes and serve smaller requests from that, we get the benefit of TCP windowing and
 * streaming from the server to the client.
 * <p>
 * (NB - This is generally not a problem on low latency links (like local or lan filesystems - which in addition have buffering and read-ahead).
 */
public class FullRangeWrapper extends WrappedStreamSource {
    private static final Logger log = LoggerFactory.getLogger(FullRangeWrapper.class);

    private static final int DEFAULT_SEEK_THRESHOLD = 1000;
    private final int seekThreshold;

    PersistentInputStream fullRangeStream;
    RequestRange streamRange;
    RequestRange lastRange;

    private StreamSourceMetadata sourceMeta;

    public FullRangeWrapper(StreamSource source) {
        this(source, DEFAULT_SEEK_THRESHOLD);
    }

    public FullRangeWrapper(StreamSource source, int seekThreshold) {
        super(source);
        this.seekThreshold = seekThreshold;
    }

    @Override
    public InputStream open(long start, long minLength) {
        // 1. If working with a full range stream - see if it's seekable - then reuse, or close
        if (fullRangeStream != null) {
            if (fullRangeStream.isClosed()) {
                long streamPos = streamRange.getFirst() + fullRangeStream.getPosition();
                long seek = start - streamPos;
                if (seek >= 0 && seek <= seekThreshold) {
                    fullRangeStream.reopen();
                    if (seek > 0) {
                        try {
                            fullRangeStream.skip(seek);
                        } catch (IOException e) {
                           throw GorResourceException.fromIOException(e, getFullPath()).retry();
                        }
                    }
                    return fullRangeStream;
                } else {
                    log.debug("Seek is {} - close full range stream", seek);
                    clearStream();
                }
            } else {
                log.debug("Cannot reuse fullRangeStream - not closed");
            }
        } else {
            // 2. If we have info on previous seek - and that's within a seekable gap - open a full range stream
            if (lastRange != null) {
                long gap = start - lastRange.getLast() - 1;
                if (gap >= 0 && gap <= seekThreshold) {
                    log.debug("Gap is {} - switch to full range", gap);
                    streamRange = RequestRange.fromFirstLast(start, getSourceMetadata().getLength() - 1);
                    lastRange = null;  // Forget that - we're not tracking this while the stream is open
                    fullRangeStream = new PersistentInputStream(super.open(streamRange.getFirst(), streamRange.getLength()));
                    return fullRangeStream;
                }
            }
            // record this range request and just open this window.
            lastRange = RequestRange.fromFirstLength(start, minLength);
        }
        // 3. Last fallback - just open base stream on this window
        return super.open(start, minLength);
    }

    private void clearStream() {
        if (fullRangeStream != null) {
            fullRangeStream.reallyClose();
            fullRangeStream = null;
            streamRange = null;
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


}
