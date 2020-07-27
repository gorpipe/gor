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
package org.gorpipe.model.util;

/**
 * Compute the log of the Gamma-function for integer values, i.e.,
 * compute log(n!) for n = 0,1,2,...
 * Computed values are stored to avoid recalculations.
 * Original version by Alfred Hauksson.
 *
 * @version $Id$
 */
public class GLogGamma {
    private static final DoubleArray logGamma = new DoubleArray();


    /**
     * Returns log(i!).
     *
     * @param i non-negative integer
     * @return log(i!)
     */
    public static double get(int i) {
        if (i >= logGamma.size()) {
            calculate(i);
        }
        return logGamma.get(i);
    }

    /**
     * Computes the values log(0!), log(1!), ..., log(n!) that have not
     * already been calculated.
     */
    private synchronized static void calculate(int n) {
        if (logGamma.size() == 0) {  // Need this if static
            logGamma.add(0.0);
        }
        for (int i = logGamma.size(); i < n + 1; ++i) {
            logGamma.add(get(i - 1) + Math.log(i));
        }
    }
}
