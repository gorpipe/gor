package gorsat.process;

import org.junit.Assert;
import org.junit.Test;
import scala.Function0;
import scala.Unit;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class UTestParallelExecutor {

    /**
     * ENGKNOW-3979: a failed command must cancel the run through an explicit flag, not only by interrupting
     * siblings. A sibling that loses its interrupt flag (e.g. code that calls Thread.interrupted() or swallows
     * InterruptedException) must still see the run as cancelled, and queued commands must not be started.
     */
    @Test
    @SuppressWarnings("unchecked")
    public void failedCommandCancelsRunEvenIfSiblingClearsInterrupt() {
        var cancelled = new AtomicBoolean(false);
        var siblingStarted = new CountDownLatch(1);
        var siblingSawCancel = new AtomicBoolean(false);
        var queuedRan = new AtomicBoolean(false);

        Function0<Unit> sibling = () -> {
            siblingStarted.countDown();
            long deadline = System.currentTimeMillis() + 10000;
            // Thread.interrupted() clears the flag, like code that swallows the interrupt
            while (!Thread.interrupted() && System.currentTimeMillis() < deadline) {
                Thread.onSpinWait();
            }
            siblingSawCancel.set(cancelled.get());
            return null;
        };
        Function0<Unit> failing = () -> {
            try {
                siblingStarted.await(10, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            throw new IllegalStateException("part failed");
        };
        Function0<Unit> queued = () -> {
            queuedRan.set(true);
            return null;
        };

        var pe = new ParallelExecutor(2, new Function0[]{sibling, failing, queued}, cancelled);

        var ex = Assert.assertThrows(IllegalStateException.class, pe::parallelExecute);
        Assert.assertEquals("part failed", ex.getMessage());
        Assert.assertTrue("Run must be marked cancelled", cancelled.get());
        Assert.assertTrue("Sibling must see cancel flag after its interrupt flag is cleared", siblingSawCancel.get());
        Assert.assertFalse("Queued command must not start after a failure", queuedRan.get());
    }
}
