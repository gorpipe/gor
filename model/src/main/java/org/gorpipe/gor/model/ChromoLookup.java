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
 * Lookup Class for chromosome names to ids
 */
public interface ChromoLookup {
    /**
     * @param id The chromosome id to convert to name
     * @return The chromosome name
     */
    String idToName(int id);

    /**
     * @param chr chromosome name
     * @return The id of the chromosome, or -1 if chr is not a known chromosome
     */
    int chrToId(String chr);

    default String chrToName(String chr) {
        int id = chrToId(chr);
        if (id >= 0) return idToName(id);
        return chr;
    }

    /**
     * @param chr chromosome name
     * @return The length of the chromosome, or -1 if chr is not a known chromosome/length
     */
    int chrToLen(String chr);

    /**
     * Given a string that starts with chr and is at least 4 char long, find the chr id of it
     *
     * @param str    The string, assumed to start with chr and be at least 4 chars long
     * @param strlen The total length of the string
     * @return The id of the chromosome or -1 if this is not a valid chromosome id.
     */
    int chrToId(CharSequence str, int strlen);

    /**
     * Assume the incoming buffer is prefixed with chromosome information that end with a tab char.
     *
     * @param buf    The buffer to read
     * @param offset The offset into the buffer to start reading from
     * @return The id of the chromosome information part in the buffer, or -1 if buffer doesn't start with a known chromosome
     */
    int prefixedChrToId(byte[] buf, int offset);

    /**
     * Assume the incoming buffer is prefixed with chromosome information that end with a tab char.
     *
     * @param buf    The buffer to read
     * @param offset The offset into the buffer to start reading from
     * @param buflen The length of the buffer
     * @return The id of the chromosome information part in the buffer, or -1 if buffer doesn't start with a known chromosome
     */
    int prefixedChrToId(byte[] buf, int offset, int buflen);

    /**
     * @return current chromosome cache
     */
    ChromoCache getChromCache();


}
