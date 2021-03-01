/*
 *  BEGIN_COPYRIGHT
 *
 *  Copyright (C) 2011-2013 deCODE genetics Inc.
 *  Copyright (C) 2013-2021 WuXi NextCode Inc.
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

package org.gorpipe.base.config.bytesize;

/**
 * Represents the possible standards that a {@link ByteSizeUnit} can have. Different standards represent different
 * "power of" values for which byte sizes are defined in.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Binary_prefix">https://en.wikipedia.org/wiki/Binary_prefix</a>
 */
public enum ByteSizeStandard {

    /**
     * The International System of Units (SI) standard. Base of 1000.
     */
    SI(1000),

    /**
     * The International Electrotechnical Commission (IEC) standard. Base of 1024.
     */
    IEC(1024);

    final int powerOf;

    ByteSizeStandard(int powerOf) {
        this.powerOf = powerOf;
    }
}
