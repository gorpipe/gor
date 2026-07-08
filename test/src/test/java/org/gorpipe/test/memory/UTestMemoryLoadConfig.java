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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UTestMemoryLoadConfig {

    @Test
    public void defaultsAreSane() {
        MemoryLoadConfig c = new MemoryLoadConfig(1000, 100, 100, 4, 8, 70, 30);
        assertEquals(1000, c.fileCount);
        assertEquals(70, c.readWritePercent);
        assertTrue(c.threads > 0);
    }

    @Test
    public void systemPropertiesOverrideDefaults() {
        System.setProperty("gor.memtest.fileCount", "5000");
        try {
            MemoryLoadConfig c = MemoryLoadConfig.fromSystemProperties();
            assertEquals(5000, c.fileCount);
        } finally {
            System.clearProperty("gor.memtest.fileCount");
        }
    }
}
