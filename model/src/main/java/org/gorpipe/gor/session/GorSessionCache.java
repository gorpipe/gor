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

package org.gorpipe.gor.session;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import org.gorpipe.gor.driver.linkfile.CachedLinkContent;
import org.gorpipe.gor.driver.providers.stream.sources.StreamSource;
import org.gorpipe.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Object that stores cache associated with a session.
 */
public class GorSessionCache {

    private final Map<String, Long> seekTimes = new ConcurrentHashMap<>();
    private final Map<String, String> headerMap = new HashMap<>();  // Synchronized on access.
    private final Map<String, Pair<String, String[]>> headerFileMap = new ConcurrentHashMap<>();
    private final Map<String, Pair<String, byte[]>> fileHeaderMap = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> singleHashMaps = new HashMap<>();  // Synchronized on access
    private final Map<String, Map<String, String[]>> multiHashMaps = new HashMap<>(); // Synchronized on access
    private final Map<String, String[]> listMaps = new HashMap<>();   // Synchronized on access
    private final Map<String, Object> objectHashMap = new ConcurrentHashMap<>();
    private final Map<String, Integer> fileSegMap = new ConcurrentHashMap<>();
    private final Map<String, Set<String>> sets = new HashMap<>();  // Synchronized on access
    // Bounded on purpose (ENGKNOW-3722): gor-worker sessions are long-lived, and an unbounded, never
    // expiring cache could serve an object length for the lifetime of the process.  A .link file
    // rewritten by another pod then leaves this session clamping ranges to a length that no longer
    // exists.  Matches the expiry of the static fallback cache in S3Source.
    private final Cache<String, Object> s3MetadataCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES).build();
    // Per session and expiring (ENGKNOW-3722): as a static cache this held link content for the whole
    // process, so content read by one session was served to every later one.  Link files get rewritten,
    // so content held past its session may no longer be true.  Matches the bounds of the static
    // fallback cache in LinkFile.
    private final Cache<String, CachedLinkContent> linkCache = Caffeine.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(5, TimeUnit.MINUTES).build();


    public Map<String, Long> getSeekTimes() {
        return seekTimes;
    }

    public Map<String, String> getHeaderMap() {
        return headerMap;
    }

    public Map<String, Pair<String, String[]>> getHeaderFileMap() {
        return headerFileMap;
    }

    public Map<String, Pair<String, byte[]>> getFileHeaderMap() {
        return fileHeaderMap;
    }

    public Map<String, Map<String, String>> getSingleHashMaps() {
        return singleHashMaps;
    }

    public Map<String, Map<String, String[]>> getMultiHashMaps() {
        return multiHashMaps;
    }

    public Map<String, String[]> getListMaps() {
        return listMaps;
    }

    public Map<String, Object> getObjectHashMap() {
        return objectHashMap;
    }

    public Map<String, Integer> getFileSegMap() {
        return fileSegMap;
    }

    public Map<String, Set<String>> getSets() {
        return sets;
    }

    public Cache<String, Object> getS3MetadataCache() {
        return s3MetadataCache;
    }
    public Cache<String, CachedLinkContent> getLinkCache() {
        return linkCache;
    }

}
