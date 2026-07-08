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
