package gorsat.monitors;

import gorsat.Analysis.ForkWrite;
import gorsat.Analysis.OutputOptions;
import gorsat.Monitors.StatsMonitor;
import gorsat.process.PipeInstance;
import org.gorpipe.gor.binsearch.GorIndexType;
import org.gorpipe.gor.monitor.GorMonitor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import scala.Option;

import java.nio.file.Path;
import java.util.zip.Deflater;

import static gorsat.TestUtils.createPipeInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StatsMonitorTest{

    @Rule
    public TemporaryFolder workDir = new TemporaryFolder();
    private Path workDirPath;

    @Before
    public void setupTest() {
        workDirPath = workDir.getRoot().toPath();
    }

    @Test
    public void testRowCountAndBytesCount() {

        var statsMonitor = new StatsMonitor();
        try (PipeInstance pipe = createPipeInstance(false)) {
            pipe.init("gorrows -p chr1:1-1000 | calc a 'abc'", null);
            pipe.lastStep().$bar(statsMonitor);

            while (pipe.hasNext()) {
                pipe.next();
            }
        }

        assertEquals("chrom\tpos\ta", statsMonitor.getHeader());
        assertEquals(999, statsMonitor.rowCount());
        assertEquals(11880, statsMonitor.bytesCount());
    }

    @Test
    public void testElapsedTime() throws InterruptedException {
        StatsMonitor monitor = new StatsMonitor();
        Thread.sleep(100);
        monitor.finish();

        long elapsedTime = monitor.elapsedTime();
        assertTrue(elapsedTime >= 100);
    }

    @Test
    public void testRowCountAndBytesCountForSelfWriting() {

        var statsMonitor = new StatsMonitor();
        try (PipeInstance pipe = createPipeInstance(false)) {
            pipe.init("gorrows -p chr1:1-1000 | calc a 'abc' | write " + workDirPath.resolve("test.gor").toString(), null);
            pipe.lastStep().$bar(statsMonitor);

            while (pipe.hasNext()) {
                pipe.next();
            }
        }

        assertEquals("", statsMonitor.getHeader());
        assertEquals(0, statsMonitor.rowCount());
        assertEquals(0, statsMonitor.bytesCount());
    }

    @Test
    public void testRowCountAndBytesCountForAddedWrite() {

        var statsMonitor = new StatsMonitor();
        ForkWrite forkWrite = null;
        try (PipeInstance pipe = createPipeInstance(true)) {
            pipe.init("gorrows -p chr1:1-1000 | calc a 'abc'", new GorMonitor());
            pipe.lastStep().$bar(statsMonitor);

            var outputOptions = new OutputOptions(false, false, true, false,
                    false,  GorIndexType.NONE, new String[0], new String[0], Option.empty(),  Option.empty(), Deflater.BEST_SPEED,
                    Option.empty(), false, false, null, "", null, false, false);
            forkWrite = new ForkWrite(-1, workDirPath.resolve("test.gor").toString(), pipe.getSession(), pipe.getHeader(), outputOptions);

            pipe.lastStep().$bar(forkWrite);

            while (pipe.hasNext()) {
                pipe.next();
            }
        }

        assertEquals("chrom\tpos\ta", statsMonitor.getHeader());
        assertEquals(999, statsMonitor.rowCount());
        assertEquals(11880, statsMonitor.bytesCount());

        assertEquals("c84f18441d00c7ea79d233efdaf8f07b", forkWrite.getMd5());
    }

}
