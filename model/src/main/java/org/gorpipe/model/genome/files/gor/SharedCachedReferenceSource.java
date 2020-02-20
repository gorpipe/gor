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

package org.gorpipe.model.genome.files.gor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import org.gorpipe.exceptions.GorDataException;
import htsjdk.samtools.SAMException;
import htsjdk.samtools.SAMSequenceRecord;
import htsjdk.samtools.cram.ref.CRAMReferenceSource;
import htsjdk.samtools.util.StringUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Used to represent a CRAM reference, the backing source for which can either be
 * a file or the EBI ENA reference service.
 *
 * NOTE: In a future release, this class will be renamed and the functionality it
 * contains will be refactored and distributed into one or more separate reference
 * source implementations, each corresponding to the type of resource backing the
 * reference.
 */

public abstract class SharedCachedReferenceSource implements CRAMReferenceSource,Closeable {

    public static final Map<String, Cache<String, byte[]>> sharedCache = new ConcurrentHashMap<>();
    private static final Logger log = LoggerFactory.getLogger("console." + SharedCachedReferenceSource.class);


    protected Cache<String, byte[]> cacheW = null;
    protected String referenceKey;


    protected SharedCachedReferenceSource(String referenceFileKey) {
        this.referenceKey = referenceFileKey;

        initializeCache();
    }

    private void initializeCache() {
        cacheW = sharedCache.computeIfAbsent(this.referenceKey, a -> createCache());
    }

    protected Cache<String, byte[]> createCache () {
        RemovalListener<String, byte[]> removalNotifier;
        removalNotifier = notification -> log.debug("Removing from cache, key: {}, cause: {}",  notification.getKey(), notification.getCause());

        // TODO: Move this to owner config or context object when the dirver model supports that
        Integer cacheTimout = Integer.parseInt(System.getProperty("gor.driver.cram.referencetimeout", "60")); // Seconds

        return CacheBuilder.newBuilder().removalListener(removalNotifier)
                .expireAfterAccess(cacheTimout, TimeUnit.SECONDS).build();
    }


    @Override
    public synchronized byte[] getReferenceBases(final SAMSequenceRecord record,
                                                 final boolean tryNameVariants) {
        // check cache by sequence name:
        final String recordName = record.getSequenceName();

        try {
            return cacheW.get(recordName, () -> computeCacheInternal(recordName, tryNameVariants));
        }catch (ExecutionException e) {
            throw new GorDataException("Failed to load CRAM reference: " + recordName, e);
        }
    }

    private byte[] computeCacheInternal(String name, boolean tryVariants) {

        log.debug("Loading reference for {}", name);
        byte[] bases = loadReference(name);

        if (tryVariants && (bases == null || bases.length == 0)) {
            for (final String variant : getVariants(name)) {
                try {
                    bases = loadReference(variant);
                } catch (final SAMException e) {
                    log.warn("Sequence not found: " + variant);
                }
                if (bases != null && bases.length > 0)
                    break;
            }
        }

        if (bases != null) {
            for (int i = 0; i < bases.length; i++) {
                bases[i] = StringUtil.toUpperCase(bases[i]);
            }
        } else {
            throw new GorDataException("Unable to load reference for chromosome " + name);
        }

        log.debug("Adding reference for {}, size {}", name, bases.length);

        return bases;
    }


    protected abstract byte[] loadReference(String name);

    @Override
    public void close() {
        cacheW = null;
    }

    private static final Pattern chrPattern = Pattern.compile("chr.*",
            Pattern.CASE_INSENSITIVE);

    protected List<String> getVariants(final String name) {
        final List<String> variants = new ArrayList<String>();

        if (name.equals("M"))
            variants.add("MT");

        if (name.equals("MT"))
            variants.add("M");

        final boolean chrPatternMatch = chrPattern.matcher(name).matches();
        if (chrPatternMatch)
            variants.add(name.substring(3));
        else
            variants.add("chr" + name);

        if ("chrM".equals(name)) {
            // chrM case:
            variants.add("MT");
        }
        return variants;
    }
}
