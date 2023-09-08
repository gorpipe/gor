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

package org.gorpipe.gor.driver;

import org.gorpipe.base.config.ConfigManager;
import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.gorpipe.gor.driver.meta.DataType;
import org.gorpipe.gor.driver.meta.IndexableSourceReference;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.StreamSourceProvider;
import org.gorpipe.gor.model.FileReader;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.model.GenomicIteratorBase;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.gor.util.DataUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.Map.Entry;

import static org.gorpipe.gor.driver.meta.DataType.LINK;

public class PluggableGorDriver implements GorDriver {
    private final static Logger log = LoggerFactory.getLogger(PluggableGorDriver.class);

    private final TreeMap<String, SourceType> protocolToSourceType = new TreeMap<>();
    private final Map<SourceType, SourceProvider> sourceTypeToSourceProvider = new HashMap<>();

    private final GorDriverConfig config;

    private static PluggableGorDriver instance;

    PluggableGorDriver(Set<SourceProvider> initialSourceProviders, GorDriverConfig config) {
        if (initialSourceProviders != null) {
            for (SourceProvider provider : initialSourceProviders) {
                register(provider);
            }
        }
        this.config = config;
    }

    public static PluggableGorDriver instance() {
        if (instance == null) {
            GorDriverConfig gorDriverConfig = ConfigManager.getPrefixConfig("gor", GorDriverConfig.class);
            FileCache cache = new FileCache(gorDriverConfig);

            ServiceLoader<StreamSourceIteratorFactory> factoryServiceLoader = ServiceLoader.load(StreamSourceIteratorFactory.class);
            Set<StreamSourceIteratorFactory> factories = new HashSet<>();
            for(StreamSourceIteratorFactory sp: factoryServiceLoader) {
                factories.add(sp);
            }

            ServiceLoader<SourceProvider> sourceProviders = ServiceLoader.load(SourceProvider.class);
            Set<SourceProvider> set = new HashSet<>();
            for(SourceProvider sp: sourceProviders) {
                sp.setConfig(gorDriverConfig);
                sp.setCache(cache);
                if (sp instanceof StreamSourceProvider) {
                    ((StreamSourceProvider) sp).setIteratorFactories(factories);
                }
                set.add(sp);
            }

            instance = new PluggableGorDriver(set, gorDriverConfig);
        }
        return instance;
    }

    private void register(SourceProvider provider) {
        // Sanity check source type - we don't want conflicts in protocols
        log.debug("Registering source provider {}", provider);
        for (SourceType type : provider.getSupportedSourceTypes()) {
            for (String protocol : type.getProtocols()) {
                SourceType existing = protocolToSourceType.get(protocol);
                if (existing != null && !existing.equals(type)) {
                    throw new GorSystemException("Cannot map protocol '" + protocol + "' to " + type + ": Already mapped to " + existing, null);
                }
            }
        }
        for (SourceType type : provider.getSupportedSourceTypes()) {
            for (String protocol : type.getProtocols()) {
                protocolToSourceType.put(protocol, type);
            }
            if (sourceTypeToSourceProvider.containsKey(type)) {
                log.warn("Overriding handling of source type {} with {}, was {}", type, sourceTypeToSourceProvider.get(type), provider);
            }
            sourceTypeToSourceProvider.put(type, provider);
        }
    }

    @Override
    public GenomicIterator createIterator(SourceReference sourceReference) throws IOException {
        DataSource source = getDataSource(sourceReference);
        if (source != null) {
            return createIterator(source);
        }
        return null;
    }

    @Override
    public GenomicIterator createIterator(DataSource source) throws IOException {
        log.debug("Create iterator for datasource {}", source);
        if (!source.exists()) {
            log.debug("Source {} reports it does not exist", source);
            throw new GorResourceException(String.format("Input source does not exist: %s", source.getName()), source.getName());
        }

        try {
            log.debug("Delegate to source provider {}", source);
            return sourceTypeToSourceProvider.get(source.getSourceType()).createIterator(source);
        } catch (Exception e) {
            throwWithSourceName(e, source.getName());
            return null;  // Never gets here
        }
    }

    @Override
    public GenomicIteratorBase createMetaIterator(DataSource source, FileReader reader) throws IOException {
        log.debug("Create meta iterator for datasource {}", source);
        if (!source.exists()) {
            log.debug("Source {} reports it does not exist", source);
            throw new GorResourceException(String.format("Input source does not exist: %s", source.getName()), source.getName());
        }

        try {
            log.debug("Delegate to source provider {}", source);
            return sourceTypeToSourceProvider.get(source.getSourceType()).createMetaIterator(source, reader);
        } catch (Exception e) {
                throwWithSourceName(e, source.getName());
            return null;  // Never gets here
        }
    }

    @Override
    public DataSource getDataSource(SourceReference sourceReference) {
        log.debug("Get data source for source reference {}", sourceReference);
        try {
            DataSource source = wrap(resolveDataSource(sourceReference));
            source = sourceReference.writeSource ? source : handleLinks(source);
            return source;
        } catch (Exception e) {
            throwWithSourceName(e, sourceReference.getUrl());
            return null;  // Never gets here
        }
    }

    @Override
    public DataSource wrap(DataSource source) throws IOException {
        if (source == null) return null;
        return sourceTypeToSourceProvider.get(source.getSourceType()).wrap(source);
    }

    @Override
    public DataSource resolveDataSource(SourceReference sourceReference) throws IOException {
        var providerFileName = sourceReference.commonRoot != null && !PathUtils.isLocal(sourceReference.commonRoot)
                ? PathUtils.resolve(sourceReference.commonRoot,sourceReference.getUrl())
                : sourceReference.getUrl();
        SourceProvider provider = providerFromFileName(providerFileName);
        return provider.resolveDataSource(sourceReference);
    }

    /**
     * Use file/source name to determine SourceType
     *
     * @return SourceType or null if not found
     */
    private SourceType typeFromFilename(String file) {
        String lowerCaseFile = file.toLowerCase();
        for (Entry<String, SourceType> entry : protocolToSourceType.descendingMap().entrySet()) {
            SourceType type = entry.getValue();
            if (type.match(lowerCaseFile)) {
                return type;
            }
        }
        return null;
    }

    private SourceProvider providerFromFileName(String file) {
        SourceType type = typeFromFilename(file);
        if (type == null) {
            throw new GorResourceException(String.format("No driver found for file %s", file), file);
        }
        return sourceTypeToSourceProvider.get(type);
    }

    @Override
    public SourceType[] getSupportedSourceTypes() {
        return sourceTypeToSourceProvider.keySet().toArray(new SourceType[0]);
    }

    /*
     * TODO: Detect circular links
     * TODO: Check what happens if we chain of links where last link in chain is non existent link file.
     */
    private DataSource handleLinks(DataSource source) throws IOException {
        if (source.getDataType() == LINK) {
            if (source.exists()) {
                var sourceRef = getSourceRef(source, readLink(source), null);
                sourceRef.setLinkLastModified(source.getSourceMetadata().getLastModified());
                source.close();
                DataSource rawLinkSource = resolveDataSource(sourceRef);
                if (rawLinkSource == null) {
                    throw new GorResourceException("Link file content" + sourceRef.getUrl() + " can not be resolved", sourceRef.getUrl());
                }
                DataSource fromLinkSource = wrap(rawLinkSource);
                fromLinkSource.getSourceReference().setCreatedFromLink(true);
                return handleLinks(fromLinkSource);
            }
        } else {
            if (source.supportsLinks() && !source.exists()) {
                return tryResolveWithLink(source);
            }
        }
        return source;
    }

    private SourceReference getSourceRef(DataSource parent, String url, String linkSubPath) {
        if (parent.getSourceReference() instanceof IndexableSourceReference) {
            return new IndexableSourceReference(url, (IndexableSourceReference)parent.getSourceReference(), linkSubPath);
        } else {
            return new SourceReference(url, parent.getSourceReference(), linkSubPath);
        }
    }

    private DataSource tryResolveWithLink(DataSource source) throws IOException {
        SourceReference fallbackSourceRef = getSourceRef(source, DataUtil.toFile( source.getSourceReference().getUrl(), LINK), null);
        DataSource fallbackLinkSource = wrap(resolveDataSource(fallbackSourceRef));
        if (fallbackLinkSource != null && fallbackLinkSource.exists()) {
            source.close();
            // The link file existed, was resolved.
            return handleLinks(fallbackLinkSource);
        } else {
            String useLinkFolders = System.getProperty("GOR_DRIVER_LINK_FOLDERS","false");
            if (useLinkFolders.equals("true")) {
                return resolveLinkFolders(source);
            }
        }
        return source;
    }

    private String fixHttpUrl(String url) {
        return url.startsWith("http") && url.contains(":/") && !url.contains("://") ? url.replace(":/","://") : url;
    }

    private DataSource resolveLinkFolders(DataSource source) throws IOException {
        Path p = Paths.get(source.getSourceReference().getUrl());
        Path newLinkSubPath = p.getFileName();
        Path parent = p.getParent();

        if (parent != null) {
            return recursiveLinkFolderFallBack(source, parent, newLinkSubPath);
        } else {
            return source;
        }
    }

    private DataSource recursiveLinkFolderFallBack(DataSource source, Path parent, Path linkSubPath) throws IOException {
        Path pparent = parent.getParent();
        if(pparent != null && !pparent.toString().endsWith(":")) {
            Path parentFileName = parent.getFileName();
            Path lparent = pparent.resolve(DataUtil.toFile(parentFileName.toString(), DataType.LINK));

            SourceReference sourceRef = getSourceRef(source, fixHttpUrl(lparent.toString()), linkSubPath!=null ? linkSubPath.toString() : null);
            DataSource fallbackLinkSource = getDataSource(sourceRef);

            if (fallbackLinkSource.getDataType() != LINK) {
                return fallbackLinkSource;
            } else {
                Path newLinkSubPath = parentFileName.resolve(linkSubPath);
                return recursiveLinkFolderFallBack(source, pparent, newLinkSubPath);
            }
        }
        return source;
    }

    @Override
    public String readLink(DataSource source) throws IOException {
        SourceReference sourceReference = source.getSourceReference();
        String linkSubPath = sourceReference.getLinkSubPath();
        String linkText = sourceTypeToSourceProvider.get(source.getSourceType()).readLink(source);
        return linkSubPath != null ? linkText + linkSubPath : linkText;
    }

    @Override
    public GorDriverConfig config() {
        return config;
    }

    private void throwWithSourceName(Exception e, String sourcename) {
        if (sourcename == null) {
            throw new GorResourceException("Gor driver sourcename is null", "", e);
        } else if (e.getMessage() == null) {
            throw new GorSystemException("Gor driver - message is null", e);
        }

        if (e instanceof GorException) {
            throw (GorException)e;
        }

        throw new GorResourceException("Cannot create iterator for datasource: " + sourcename + "  Cause: "
                + e.getMessage(), sourcename, e);
    }
}
