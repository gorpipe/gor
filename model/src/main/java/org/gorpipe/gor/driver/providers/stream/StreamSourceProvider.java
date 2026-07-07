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

package org.gorpipe.gor.driver.providers.stream;

import org.gorpipe.exceptions.ExceptionUtilities;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.*;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.CachedSourceWrapper;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.ExtendedRangeWrapper;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.FullRangeWrapper;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.RetryStreamSourceWrapper;
import org.gorpipe.gor.driver.utils.RetryHandlerBase;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.GenomicIteratorBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public abstract class StreamSourceProvider implements SourceProvider {
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<DataType, StreamSourceIteratorFactory> dataTypeToFactory = new HashMap<>();
    private FileCache cache;
    protected GorDriverConfig config;

    protected RetryHandlerBase retryHandler;

    public StreamSourceProvider() {}

    public StreamSourceProvider(GorDriverConfig config, FileCache cache, Set<StreamSourceIteratorFactory> initialFactories) {
        this.config = config;
        this.cache = cache;
        for (StreamSourceIteratorFactory factory : initialFactories) {
            register(factory);
        }
    }

    public void setConfig(GorDriverConfig config) {
        this.config = config;
    }

    @Override
    public void setCache(FileCache cache) {
        this.cache = cache;
    }

    public void setIteratorFactories(Set<StreamSourceIteratorFactory> factories) {
        for (StreamSourceIteratorFactory factory : factories) {
            register(factory);
        }
    }

    @Override
    abstract public StreamSource resolveDataSource(SourceReference sourceReference) throws IOException;

    private void register(StreamSourceIteratorFactory factory) {
        for (DataType type : factory.getSupportedDataTypes()) {
            if (dataTypeToFactory.containsKey(type)) {
                log.warn("Overriding handling of data type {} with {}, was {}", type, dataTypeToFactory.get(type), factory);
            }
            dataTypeToFactory.put(type, factory);
        }
    }

    /**
     * Wrap core data source with wrappers providing extended functionality
     */
    @Override
    public StreamSource wrap(DataSource input) throws IOException {
        StreamSource source = (StreamSource) input;
        // Wrap with retry logic before full range streaming because retries will look like seeks - no longer looking like sequential reads
        if (config.retriesEnabled()) {
            source = new RetryStreamSourceWrapper(getRetryHandler(), source);
        }

        if (source.getSourceType().isRemote()) {
            if (config.remoteExtendedRangeStreamingEnabled()) {
                log.debug("Wrapping remote source with ExtendedRangeWrapper");
                source = new ExtendedRangeWrapper(source, config.extendedRangeStreamingSeekThreshold().getBytesAsInt(),
                        config.extendedRangeStreamingMaxRequestSize().getBytesAsInt());
            } else if (config.remoteFullRangeStreamingEnabled()) {
                log.debug("Wrapping remote source with FullRangeWrapper");
                source = new FullRangeWrapper(source);
            }

        }

        // Source type could have changed in the above clause - a cached remote source is effectively local
        // Thus check source type again before applying local wrappers.
        if (!source.getSourceType().isRemote()) {
            if (config.localExtendedRangeStreamingEnabled()) {
                log.debug("Wrapping local source with ExtendedRangeWrapper");
                source = new ExtendedRangeWrapper(source, config.extendedRangeStreamingSeekThreshold().getBytesAsInt(),
                        config.extendedRangeStreamingMaxRequestSize().getBytesAsInt());
            } else if (config.localFullRangeStreamingEnabled()) {
                log.debug("Wrapping local source with FullRangeWrapper");
                source = new FullRangeWrapper(source);
            }
        }

        DataType dataType = source.getDataType();
        if (dataType != null && dataType.nature == FileNature.INDEX && source.exists()) {
            log.debug("Detected index source {}", source.getName());
            Long length = source.getSourceMetadata().getLength();
            if (length != null && length < config.maxSizeOfCachedIndexFile().getBytesAsLong()) {
                log.debug("Caching index source {} of length {}", source.getName(), length);
                source = new CachedSourceWrapper(cache, source);
            } else {
                log.debug("Not caching index source {} length {} is more than max of {}", source.getName(), length, config.maxSizeOfCachedIndexFile());
            }
        }
        return source;
    }

    protected abstract RetryHandlerBase getRetryHandler();

    /**
     * Create genomic iterator from data source
     */
    @Override
    public GenomicIterator createIterator(DataSource source) throws IOException {
        StreamSourceIteratorFactory factory = getFactory(source);
        StreamSourceFile file = factory != null ? factory.resolveFile((StreamSource)source) : null;
        if (file == null) {
            return null;
        }

        SourceReference sourceRef = source.getSourceReference();
        if (sourceRef instanceof IndexableSourceReference) {
            IndexableSourceReference iSourceRef = (IndexableSourceReference) sourceRef;

            if (file.supportsIndex()) {
                // Test for index source
                String indexUrl = iSourceRef.getIndexSource();
                StreamSource idxSource;

                if (indexUrl != null) {
                    idxSource = (StreamSource) GorDriverFactory.fromConfig().getDataSource(new SourceReference(indexUrl, sourceRef));
                } else {
                    // Find from the driver
                    idxSource = findIndexFileFromFileDriver(file, sourceRef);
                }

                file.setIndexSource(idxSource);
            }

            // Test for reference source
            if (file.supportsReference()) {
                String referenceUrl = iSourceRef.getReferenceSource();

                // If reference file is not supplied with -ref command ask the driver for
                // the assigned reference file
                if (referenceUrl == null || referenceUrl.isEmpty()) {
                    referenceUrl = file.getReferenceFileName();
                }

                StreamSource refSource = null;
                if (referenceUrl != null) {
                    refSource = (StreamSource) GorDriverFactory.fromConfig().getDataSource(new SourceReference(referenceUrl, sourceRef));
                }

                file.setReferenceSource(refSource);
            }

        } else {
            if (file.supportsIndex() && file.getIndexSource() == null) {
                StreamSource idxSource = findIndexFileFromFileDriver(file, sourceRef);

                if (idxSource != null) {
                    file.setIndexSource(idxSource);
                }
            }
        }

        return factory.createIterator(file);
    }

    @Override
    public GenomicIteratorBase createMetaIterator(DataSource source, FileReader reader) throws IOException {
        DataType type = source.getDataType();
        if (type == null) {
            log.warn("Unknown DataType for {}", source.getName());
        }
        log.debug("GorDriver: Datatype of {} is {}", source.getName(), type);

        StreamSourceIteratorFactory factory = dataTypeToFactory.get(source.getDataType());
        if (factory != null) {
            StreamSourceFile file = factory.resolveFile((StreamSource) source);
            file.setIndexSource(findIndexFileFromFileDriver(file, source.getSourceReference()));
            var factoryMetaIt = factory.createMetaIterator(file, reader);
            var sourceMetaIt = new SourceMetaIterator();
            sourceMetaIt.initMeta(source);
            sourceMetaIt.merge(factoryMetaIt);

            return sourceMetaIt;
        }

        log.warn("Unsupported datatype {} for source {}, using generic data source.", type, source.getName());

        // No data handlers, we should revert to reading the file directly
        var sourceMetaIt = new SourceMetaIterator();
        sourceMetaIt.initMeta(source);

        if (Objects.equals(source.getSourceType().getName(), "FILE") && source instanceof StreamSource) {
            // This is a file source so we can explorer its file properties
            var fileIt = new FileMetaIterator();
            fileIt.initMeta(new StreamSourceFile((StreamSource)source));
            sourceMetaIt.merge(fileIt);
        }

        return sourceMetaIt;
    }

    public StreamSourceFile getSourceFile(DataSource source) throws IOException {
        var factory = getFactory(source);
        return factory != null ? factory.resolveFile((StreamSource) source) : null;
    }

    private StreamSourceIteratorFactory getFactory(DataSource source) {
        DataType type = source.getDataType();
        if (type == null) {
            log.warn("Unknown DataType for {}", source.getName());
            return null;
        }
        log.debug("GorDriver: Datatype of {} is {}", source.getName(), type);
        StreamSourceIteratorFactory factory = dataTypeToFactory.get(source.getDataType());
        if (factory == null) {
            log.warn("Unsupported datatype {} for source {}", type, source.getName());
            return null;
        }
        return factory;
    }

    /**
     * Locate the index file for {@code file}, treating the index as optional: if its existence cannot be
     * determined (a probe error surviving retries), the index is skipped rather than failing the caller.
     * This is the query-read contract -- a missing index only degrades to a full scan (ENGKNOW-3643).
     */
    public StreamSource findIndexFileFromFileDriver(StreamSourceFile file, SourceReference sourceRef) throws IOException {
        return findIndexFileFromFileDriver(file, sourceRef, true);
    }

    /**
     * Locate the index file for {@code file}.
     *
     * @param optional when true, a probe error that survives retries is logged and the index skipped (query
     *                 read path -- a missing index is non-fatal).  When false, such an error propagates:
     *                 callers that mutate storage (copy/move/delete enumerating dependents) must get a
     *                 definitive answer, because silently skipping an index that actually exists leaves a
     *                 stale orphan {@code .gori} behind.
     */
    public StreamSource findIndexFileFromFileDriver(StreamSourceFile file, SourceReference sourceRef, boolean optional) throws IOException {
        for (String index : file.possibleIndexNames()) {
            // resolveDataSource stays outside the try: a failure here is a real configuration error
            // (bad security context, unknown scheme), not a missing optional index, so it must surface.
            StreamSource indexSource = resolveDataSource(new SourceReference(index, sourceRef));
            if (indexSource == null) {
                continue;
            }
            // Wrap before probing so the retry handler (added by wrap) covers exists(): a transient
            // storage error (e.g. an S3 503 SlowDown) is then retried instead of being silently treated
            // as "index absent" -- which would degrade an indexed query to a full scan, or fail later
            // for formats where the index is required. Only after retries are exhausted do we fall back.
            StreamSource wrapped = wrap(indexSource);
            try {
                if (wrapped.exists()) {
                    return wrapped;
                }
            } catch (Exception e) {
                if (!optional) {
                    // Caller needs a definitive answer (see javadoc) -- do not swallow.
                    if (e instanceof IOException ioe) throw ioe;
                    if (e instanceof RuntimeException re) throw re;
                    throw new IOException(e);
                }
                // Retries exhausted. The index is most often optional, so log and try the next candidate.
                // Log a one-line cause summary at WARN (this runs per candidate per file open, so full
                // stack traces here flood the log during a storage brownout); keep the detail at DEBUG.
                log.warn("Ignoring optional index candidate {} - could not determine existence after retries: {}",
                        index, ExceptionUtilities.getFullCause(e));
                log.debug("Index probe failure detail for {}", index, e);
            }
        }

        return null;
    }
}
