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
import org.gorpipe.gor.driver.meta.IndexableSourceReference;
import org.gorpipe.gor.driver.meta.SourceReference;
import org.gorpipe.gor.driver.meta.SourceType;
import org.gorpipe.gor.driver.providers.stream.FileCache;
import org.gorpipe.gor.driver.providers.stream.StreamSourceIteratorFactory;
import org.gorpipe.gor.driver.providers.stream.StreamSourceProvider;
import org.gorpipe.gor.model.GenomicIterator;
import org.gorpipe.gor.table.util.PathUtils;
import org.gorpipe.util.standalone.GorStandalone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
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
            GorDriverConfig gorDriverConfig = ConfigManager.createPrefixConfig("gor", GorDriverConfig.class);
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
            throw new FileNotFoundException(source.getName());
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
    public DataSource getDataSource(SourceReference sourceReference) {
        log.debug("Get data source for source reference {}", sourceReference);
        try {
            DataSource source = resolveDataSource(sourceReference);
            if (source == null) {
                log.debug("No source found for {}", sourceReference);
                return null;
            }
            log.debug("Datasource for {} is {}", sourceReference.getUrl(), source);
            DataSource wrapped = sourceReference.writeSource ? source : wrap(handleLinks(source));
            log.debug("Wrapped datasource for {} is {}", sourceReference.getUrl(), wrapped);
            return wrapped;
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
        if (sourceReference.commonRoot==null||PathUtils.isLocal(sourceReference.commonRoot)) {
            SourceProvider provider = providerFromFileName(sourceReference.getUrl());
            if (provider != null) {
                return provider.resolveDataSource(sourceReference);
            }
        } else {
            var fileName = PathUtils.resolve(sourceReference.commonRoot,sourceReference.getUrl());
            SourceProvider provider = providerFromFileName(fileName);
            if (provider != null) {
                return provider.resolveDataSource(sourceReference);
            }
        }
        return null;
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
        if (type != null) {
            return sourceTypeToSourceProvider.get(type);
        }
        return null;
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
                DataSource fromLinkSource = getDataSource(getSourceRef(source, readLink(source), null));
                fromLinkSource.getSourceReference().setCreatedFromLink(true);
                return fromLinkSource;
            }
        } else {
            if (source.supportsLinks() && !source.exists()) {
                String url = source.getSourceReference().getUrl();
                // prevent stackoverflow, some datasources don't support links (dbsource), need to check file ending
                if (!url.endsWith(".link")) {
                    return resolveLink(source, url);
                }
            }
        }
        return source;
    }

    private SourceReference getSourceRef(DataSource source, String url, String linkSubPath) {
        if (source.getSourceReference() instanceof IndexableSourceReference) {
            return new IndexableSourceReference(url, (IndexableSourceReference)source.getSourceReference(), linkSubPath);
        } else {
            return new SourceReference(url, source.getSourceReference(), linkSubPath);
        }
    }

    private DataSource resolveLink(DataSource source, String url) throws IOException {
        SourceReference sourceRef = getSourceRef(source, url+".link", null);

        DataSource fallbackLinkSource = getDataSource(sourceRef);
        if (fallbackLinkSource.getDataType() != LINK) {
            // The link file existed, was resolved.
            // Can not check for existance fallbackLinkSource as we allow the datasource not to exist at this point.
            return fallbackLinkSource;
        } else {
            String useLinkFolders = System.getProperty("GOR_DRIVER_LINK_FOLDERS","false");
            if (useLinkFolders.equals("true")) {
                return resolveLinkFolders(source, url);
            }
        }
        return source;
    }

    private String fixHttpUrl(String url) {
        return url.startsWith("http") && url.contains(":/") && !url.contains("://") ? url.replace(":/","://") : url;
    }

    private DataSource resolveLinkFolders(DataSource source, String url) throws IOException {
        Path p = Paths.get(url);
        Path newLinkSubPath = p.getFileName();
        Path parent = p.getParent();

        if (parent != null) {
            return recursiveLinkFallBack(source, parent, newLinkSubPath);
        } else {
            return source;
        }
    }

    private DataSource recursiveLinkFallBack(DataSource source, Path parent, Path linkSubPath) throws IOException {
        Path pparent = parent.getParent();
        if(pparent != null && !pparent.toString().endsWith(":")) {
            Path parentFileName = parent.getFileName();
            Path lparent = pparent.resolve(parentFileName+".link");

            SourceReference sourceRef = getSourceRef(source, fixHttpUrl(lparent.toString()), linkSubPath!=null ? linkSubPath.toString() : null);
            DataSource fallbackLinkSource = getDataSource(sourceRef);

            if (fallbackLinkSource.getDataType() != LINK) {
                return fallbackLinkSource;
            } else {
                Path newLinkSubPath = parentFileName.resolve(linkSubPath);
                return recursiveLinkFallBack(source, pparent, newLinkSubPath);
            }
        }
        return source;
    }

    @Override
    public String readLink(DataSource source) throws IOException {
        SourceReference sourceReference = source.getSourceReference();
        String linkSubPath = sourceReference.getLinkSubPath();
        String linkText = sourceTypeToSourceProvider.get(source.getSourceType()).readLink(source);
        if (GorStandalone.isStandalone() && !linkText.startsWith("//db:")) {
            String prefix = "";
            if (linkText.startsWith("file://")) {
                prefix = "file://";
                linkText = linkText.substring(prefix.length());
            }
            linkText = prefix + (PathUtils.isLocal(linkText) ? GorStandalone.getRootPrefixed(linkText) : linkText);
        }
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
