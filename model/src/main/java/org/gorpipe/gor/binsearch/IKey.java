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

package org.gorpipe.gor.binsearch;

/**
 * Key Generator Interface
 */
public interface IKey extends Comparable<IKey> {

    /**
     * creates a comparable key from a buffer
     *
     * @param buffer      Input buffer
     * @param bufLength   The number of valid bytes in the buffer, i.e. the buffer length must be &le; buffer.length
     * @param beginOfLine start of line in buffer
     * @return a new key parsed from the buffer
     */
    IKey createKey(byte[] buffer, int bufLength, int beginOfLine);

    /**
     * @return The key as a byte array
     */
    byte[] getKey();

    /**
     * Derive a multiplication coefficient that gives percentage of this key distance from the
     * left key in relation to the overall distance between right and left keys
     *
     * @param left  The left key, must be less or equal to this key
     * @param right The right key, must be greater or equal to this key
     * @return The distantce coeffiecent (on the scale 0..1) for the key on the total distance bewteeen left and right.
     */
    <T extends IKey> float deriveCoefficient(T left, T right);
}
