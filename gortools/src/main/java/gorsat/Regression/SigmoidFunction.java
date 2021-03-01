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

package gorsat.Regression;

import org.apache.commons.math3.util.FastMath;

import java.lang.ref.WeakReference;

/**
 * A class which uses a lookup table to compute the sigmoid function {@literal x -> 1/(1 + e^(-x))} up to a precision of 10^(-10).
 */

public class SigmoidFunction {
    private static WeakReference<SigmoidFunction> ref;

    private final double[] lookupTable;

    private SigmoidFunction() {
        this.lookupTable = new double[2_400_000];
        for (int i = 0; i < lookupTable.length; ++i) {
            lookupTable[i] = 1D / (1D + FastMath.exp(-i * 1e-5D));
        }
    }

    public synchronized static SigmoidFunction getSigmoidFunction() {
        SigmoidFunction sf;
        if (ref == null || (sf = ref.get()) == null) {
            sf = new SigmoidFunction();
            ref = new WeakReference<>(sf);
        }
        return sf;
    }

    public double sigmoid(double x) {
        if (x < 0) {
            if (x < -24) {
                return 0;
            } else {
                final double px = -x;
                final int idx = (int) (1e+5 * px);
                final double offset = px - idx * 1e-5;
                final double est = this.lookupTable[idx];
                return 1 - (est * (1 + (1 - est) * offset));
            }
        } else {
            if (x > 24) {
                return 1;
            } else {
                final int idx = (int) (1e+5 * x);
                final double offset = x - idx * 1e-5;
                final double est = this.lookupTable[idx];
                return est * (1 + (1 - est) * offset);
            }
        }
    }
}
