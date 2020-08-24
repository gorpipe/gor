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

import org.gorpipe.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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
}
