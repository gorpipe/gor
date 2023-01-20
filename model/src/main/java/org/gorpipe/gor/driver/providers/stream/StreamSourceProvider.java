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

import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.gor.driver.DataSource;
import org.gorpipe.gor.driver.GorDriverConfig;
import org.gorpipe.gor.driver.GorDriverFactory;
import org.gorpipe.gor.driver.SourceProvider;
import org.gorpipe.gor.driver.meta.*;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.gor.driver.providers.stream.sources.file.FileSource;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.CachedSourceWrapper;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.ExtendedRangeWrapper;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.FullRangeWrapper;
import org.gorpipe.gor.driver.providers.stream.sources.wrappers.RetryWrapper;
import org.gorpipe.gor.driver.utils.RetryHandler;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.GenomicIteratorBase;
import org.gorpipe.gor.model.RowBase;
import org.gorpipe.gor.util.DynamicRowIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.FileSystemException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public abstract class StreamSourceProvider implements SourceProvider {
    protected final Logger log = LoggerFactory.getLogger(getClass());
    private final Map<DataType, StreamSourceIteratorFactory> dataTypeToFactory = new HashMap<>();
    private FileCache cache;
    protected GorDriverConfig config;

    protected RetryHandler retryHandler;

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
     * Read link and resolve content.
     *
     * @return Path linked to
     */
    @Override
    public String readLink(DataSource source) throws IOException {
        String path = StreamUtils.readString((StreamSource) source, 10000);
        if (source instanceof FileSource) { //FileSource handling is a special case due to FileSource.close() implementation
            source.close();
        }
        if (path.length() < 10000) {
            path = path.trim();
            // Figure out if relative path. Assume all relative paths have no colon and don't start with /
            if (!source.getSourceType().isAbsolutePath(path)) {
                throw new IllegalArgumentException("Link file " + source.getName() + " contains relative path: " + path);
                // Allow relative links:  Just commented out if we would like to re-enable them.
                // path = source.getSourceType().resolveRelativePath(source.getName(), path);
            }
            return path;
        } else {
            throw new IllegalArgumentException("Link file " + source.getName() + " has at least 10000 bytes - aborting");
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
            source = new RetryWrapper(getRetryHandler(), source, config.maxRequestRetry(), config.maxReadRetries());
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

    protected RetryHandler getRetryHandler() {
        if (retryHandler == null) {
            retryHandler = new RetryHandler(config.retryInitialSleep().toMillis(),
                    config.retryMaxSleep().toMillis(), config.retryExpBackoff(),
                    (e) -> {
                        if (e instanceof FileNotFoundException) {
                            throw e;
                        }
                        if (e instanceof FileSystemException) {
                            throw e;
                        }
                    });
        }
        return retryHandler;
    }

    /**
     * Create genomic iterator from data source
     */
    @Override
    public GenomicIterator createIterator(DataSource source) throws IOException {
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

        StreamSourceFile file = factory.resolveFile((StreamSource) source);

        SourceReference sourceRef = source.getSourceReference();
        if (sourceRef instanceof IndexableSourceReference) {
            IndexableSourceReference iSourceRef = (IndexableSourceReference) sourceRef;

            if (file.supportsIndex()) {
                // Test for index source
                String indexUrl = iSourceRef.getIndexSource();
                StreamSource idxSource;

                if (indexUrl != null) {
                    idxSource = (StreamSource) GorDriverFactory.fromConfig().getDataSource(new SourceReference(indexUrl));
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
                    refSource = (StreamSource) GorDriverFactory.fromConfig().getDataSource(new SourceReference(referenceUrl));
                }

                file.setReferenceSource(refSource);
            }

        } else {
            if (file.supportsIndex() && file.getIndexSource() == null) {
                if (source.getSourceMetadata().isSubset()) {
                    throw new UnsupportedOperationException("Cannot handle indexed file on top of subset source");
                }

                StreamSource idxSource = findIndexFileFromFileDriver(file, sourceRef);

                if (idxSource != null) {
                    file.setIndexSource(idxSource);
                }
            }
        }

        return factory.createIterator(file);
    }

    @Override
    public GenomicIteratorBase createMetaIterator(DataSource source) throws IOException {
        DataType type = source.getDataType();
        if (type == null) {
            log.warn("Unknown DataType for {}", source.getName());
        }
        log.debug("GorDriver: Datatype of {} is {}", source.getName(), type);

        StreamSourceIteratorFactory factory = dataTypeToFactory.get(source.getDataType());
        if (factory != null) {
            StreamSourceFile file = factory.resolveFile((StreamSource) source);
            file.setIndexSource(findIndexFileFromFileDriver(file, source.getSourceReference()));
            var factoryMetaIt = factory.createMetaIterator(file);
            var sourceMetaIt = new SourceMetaIterator();
            sourceMetaIt.initMeta(source);
            sourceMetaIt.merge(factoryMetaIt);

            return sourceMetaIt;
        }

        log.warn("Unsupported datatype {} for source {}, using generic data source.", type, source.getName());

        // No data handlers, we should revert to reading the file directly
        var sourceMetaIt = new SourceMetaIterator();
        sourceMetaIt.initMeta(source);

        if (source.getSourceType().getName() == "FILE" && source instanceof StreamSource) {
            // This is a file source so we can explorer its file properties
            var fileIt = new FileMetaIterator();
            fileIt.initMeta(new StreamSourceFile((StreamSource)source));
            sourceMetaIt.merge(fileIt);
        }

        return sourceMetaIt;
    }

    private StreamSource findIndexFileFromFileDriver(StreamSourceFile file, SourceReference sourceRef) throws IOException {
        for (String index : file.possibleIndexNames()) {
            StreamSource indexSource = wrap(resolveDataSource(new SourceReference(index, sourceRef)));
            if (indexSource != null && indexSource.fileExists()) {
                indexSource = wrap(indexSource);
                if (indexSource.exists()) {
                    return indexSource;
                }
            }
        }

        return null;
    }
}
