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

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class UTestTableServiceLoadMain {

    @Test
    public void runOnceProducesSummary() throws Exception {
        Path root = Files.createTempDirectory("memtest-main");
        MemoryLoadConfig config = new MemoryLoadConfig(30, 10, 10, 2, 4, 70, 2);
        String summary = TableServiceLoadMain.runOnce(config, root);
        assertTrue("summary reports peak heap", summary.contains("peakHeapMB="));
        assertTrue("summary reports reads", summary.contains("reads="));
    }
}
