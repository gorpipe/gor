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

package org.gorpipe.test.memory;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

public class UTestMemorySampler {

    @Test
    public void recordsPeakHeapAfterAllocation() throws Exception {
        MemorySampler sampler = new MemorySampler(50);
        sampler.start();
        byte[][] hold = new byte[64][];
        for (int i = 0; i < hold.length; i++) {
            hold[i] = new byte[1024 * 1024]; // 64 MB total
            Thread.sleep(5);
        }
        sampler.stop();
        assertTrue("peak heap should exceed 32MB", sampler.peakHeapUsedBytes() > 32L * 1024 * 1024);
        assertTrue("hold retained", hold[0][0] == 0); // keep alive
    }
}
