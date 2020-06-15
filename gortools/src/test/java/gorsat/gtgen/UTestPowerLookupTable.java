package gorsat.gtgen;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static gorsat.gtgen.PowerLookupTable.pow;

public class UTestPowerLookupTable {

    @Test
    public void testPow() {
        Assert.assertEquals(1.0, pow(10.0, 0), 0);
        Assert.assertEquals(10.0, pow(10.0, 1), 0);
        Assert.assertEquals(100.0, pow(10.0, 2), 0);
        Assert.assertEquals(1000.0, pow(10.0, 3), 0);
        Assert.assertEquals(10000.0, pow(10.0, 4), 0);

        Assert.assertEquals(1.0, pow(0.5, 0), 0);
        Assert.assertEquals(0.5, pow(0.5, 1), 0);
        Assert.assertEquals(0.25, pow(0.5, 2), 0);
        Assert.assertEquals(0.125, pow(0.5, 3), 0);
        Assert.assertEquals(0.0625, pow(0.5, 4), 0);
    }

    @Test
    public void testPowerLookupTable_threadSafety() throws ExecutionException, InterruptedException {
        final int numberOfThreads = 10;
        final ExecutorService es = Executors.newFixedThreadPool(numberOfThreads);
        final double x = 0.1;

        final List<PowerLookupTable> tables = Collections.synchronizedList(new ArrayList<>());
        final Future[] futures = new Future[numberOfThreads];
        for (int i = 0; i < numberOfThreads; ++i) {
            futures[i] = es.submit(() -> {
                final PowerLookupTable plt = PowerLookupTable.getLookupTable(x);
                tables.add(plt);
            });
        }

        for (Future f : futures) {
            f.get();
        }

        final PowerLookupTable refTable = PowerLookupTable.getLookupTable(x);
        tables.forEach(table -> Assert.assertSame(refTable, table));
        es.shutdown();
    }

    @Test
    public void testGetPowFromTable() {
        final PowerLookupTable plt = PowerLookupTable.getLookupTable(1);

        Assert.assertEquals(1, plt.pow(0), 0);
        Assert.assertEquals(1, plt.pow(1000), 0);
    }
}
