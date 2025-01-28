package org.gorpipe.base.concurrency;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

public class CommonThreadPools {

    // See: java/util/concurrent/ForkJoinPool.java
    public static final ForkJoinPool seekThreadPool = new ForkJoinPool(
            Runtime.getRuntime().availableProcessors(),  // Same as default.
            ForkJoinPool.defaultForkJoinWorkerThreadFactory,  // Same as default.
            null,  // Same as default.
            false,  // Same as default.
            16,  // Defaults to the same as parallelism.
            256,  // Same as default.
            1,  // Same as default.
            p -> true,  // Set to true, so we don't throw error if pool exhausted, instead we wait.
            60,        // Same as default
            TimeUnit.SECONDS);      // Same as default

}
