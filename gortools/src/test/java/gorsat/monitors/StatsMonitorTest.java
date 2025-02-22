package gorsat.monitors;

import gorsat.Monitors.StatsMonitor;
import gorsat.process.PipeInstance;
import org.junit.Test;

import static gorsat.TestUtils.createPipeInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StatsMonitorTest{

    @Test
    public void testRowCountAndBytesCount() {

        var statsMonitor = new StatsMonitor();
        try (PipeInstance pipe = createPipeInstance(false)) {
            pipe.init("gorrows -p chr1:1-1000", null);
            pipe.lastStep().$bar(statsMonitor);

            while (pipe.hasNext()) {
                pipe.next();
            }
        }

        assertEquals("chrom\tpos", statsMonitor.getHeader());
        assertEquals(999, statsMonitor.rowCount());
        assertEquals(7884, statsMonitor.bytesCount());
    }

    @Test
    public void testElapsedTime() throws InterruptedException {
        StatsMonitor monitor = new StatsMonitor();
        Thread.sleep(100);
        monitor.finish();

        long elapsedTime = monitor.elapsedTime();
        assertTrue(elapsedTime >= 100);
    }
}
