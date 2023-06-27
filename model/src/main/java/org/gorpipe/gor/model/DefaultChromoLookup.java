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

package org.gorpipe.gor.model;

/**
 * Naive implementation of the DefaultChromoLookup
 */
public class DefaultChromoLookup implements ChromoLookup {

    private final ChromoCache lookupCache;

    public DefaultChromoLookup() {
        lookupCache = new ChromoCache();
    }

    @Override
    public final String idToName(int id) {
        return lookupCache.toName(id);
    }

    @Override
    public final int chrToId(String chr) {
        return lookupCache.toIdOrUnknown(chr, true);
    }

    @Override
    public final int chrToLen(String chr) {
        return lookupCache.toLen(chr);
    }

    @Override
    public final int chrToId(CharSequence str, int strlen) {
        return lookupCache.toIdOrUnknown(str, strlen, true);
    }

    @Override
    public final int prefixedChrToId(byte[] buf, int offset) {
        return lookupCache.prefixedChrToIdOrUnknown(buf, offset, true);
    }

    @Override
    public final int prefixedChrToId(byte[] buf, int offset, int buflen) {
        return lookupCache.prefixedChrToIdOrUnknown(buf, offset, buflen, true);
    }

    @Override
    public ChromoCache getChromoCache() {
        return lookupCache;
    }
}