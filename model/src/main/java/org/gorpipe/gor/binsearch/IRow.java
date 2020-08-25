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
 * Row generation interface.  Row is generated from a byte array
 *
 * @param <T> The type of the Row
 */
public interface IRow<T> extends IKey {
    /**
     * @return the row in bytes
     */
    byte[] getRow();

    /**
     * @return The source of the row
     */
    IRowSource<T> getSource();

    /**
     * @param source      The source of the input buffer
     * @param buffer      The input buffer containing row data
     * @param beginOfLine begin of a line within the input buffer
     * @return a new row created from a byte array
     */
    T createRow(IRowSource<T> source, byte[] buffer, int beginOfLine);

}
