package org.gorpipe.gor.driver.utils;

import org.gorpipe.exceptions.GorException;
import org.gorpipe.exceptions.GorResourceException;
import org.gorpipe.exceptions.GorSystemException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

/** Retry loop behaviour shared by every fixed-wait handler (ENGKNOW-3723). */
public class UTestRetryHandlerWithFixedWait {

    static class RecordingHandler extends RetryHandlerWithFixedWait {
        final List<Long> sleeps = new ArrayList<>();
        final List<String> retriedOps = new ArrayList<>();
        final List<String> gaveUpOps = new ArrayList<>();
        long retryAfter = 0;

        RecordingHandler(long initial, long total) {
            super(initial, total);
            // No jitter: nextDouble() == 1.0 makes the default quadratic formula deterministic.
            this.rand = new Random() {
                @Override
                public double nextDouble() {
                    return 1.0;
                }
            };
        }

        @Override protected void checkIfShouldRetryException(GorException e) { }
        @Override protected void threadSleep(long ms, int tries, Throwable t) { sleeps.add(ms); }
        @Override protected long retryAfterMillis(GorException e) { return retryAfter; }
        @Override protected void onRetry(String op, GorException e, int attempt, long sleepMs) { retriedOps.add(op); }
        @Override protected void onGiveUp(String op, GorException e, int attempts, long total) { gaveUpOps.add(op); }
    }

    private static GorResourceException retryable() {
        return (GorResourceException) new GorResourceException("boom", "s3://bucket/dir/file.gorz").retry();
    }

    private static RetryHandlerBase.Action<String> failTimes(int failures, AtomicInteger attempts) {
        return () -> {
            if (attempts.incrementAndGet() <= failures) throw retryable();
            return "ok";
        };
    }

    @Test
    public void noSleepBeforeGivingUp() {
        // Quadratic default: 1000, 4000 (total 5000 == budget), the next 9000 would exceed it.
        var handler = new RecordingHandler(1000, 5000);
        var attempts = new AtomicInteger();

        var e = assertThrows(GorSystemException.class, () -> handler.perform(failTimes(100, attempts)));

        assertEquals(List.of(1000L, 4000L), handler.sleeps);
        assertEquals(3, attempts.get());
        assertEquals(List.of("unknown"), handler.gaveUpOps);
        assertTrue(e.getMessage().contains("Giving up after"));
    }

    @Test
    public void quadraticBackoffStillDefaultForOtherHandlers() {
        var handler = new RecordingHandler(2000, 120_000);
        var attempts = new AtomicInteger();

        assertThrows(GorSystemException.class, () -> handler.perform(failTimes(100, attempts)));

        // Same six attempts as before ENGKNOW-3723, minus the pointless 72 s sleep before giving up.
        assertEquals(List.of(2000L, 8000L, 18000L, 32000L, 50000L), handler.sleeps);
        assertEquals(6, attempts.get());
    }

    @Test
    public void retryAfterRaisesSleep() {
        var handler = new RecordingHandler(1000, 100_000);
        handler.retryAfter = 3000;

        assertEquals("ok", handler.perform(failTimes(1, new AtomicInteger())));
        assertEquals(List.of(3000L), handler.sleeps);
    }

    @Test
    public void retryAfterBeyondBudgetGivesUpWithoutSleeping() {
        var handler = new RecordingHandler(1000, 10_000);
        handler.retryAfter = 60_000;

        assertThrows(GorSystemException.class, () -> handler.perform(failTimes(100, new AtomicInteger())));
        assertTrue(handler.sleeps.isEmpty());
    }

    @Test
    public void operationLabelReachesHooks() {
        var handler = new RecordingHandler(10, 10_000);

        handler.perform("metadata", failTimes(1, new AtomicInteger()));
        handler.perform(failTimes(1, new AtomicInteger()));

        assertEquals(List.of("metadata", "unknown"), handler.retriedOps);
    }

    @Test
    public void voidActionRetriesAndRunsPreRetryOp() {
        var handler = new RecordingHandler(10, 10_000);
        var attempts = new AtomicInteger();
        var preRetry = new AtomicInteger();

        handler.perform("read", () -> {
            if (attempts.incrementAndGet() == 1) throw retryable();
        }, preRetry::incrementAndGet);

        assertEquals(2, attempts.get());
        assertEquals(1, preRetry.get());
        assertEquals(List.of("read"), handler.retriedOps);
    }
}
