package com.github.uright008.pc;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Production {@link WriteQueue} backed by a {@link ConcurrentLinkedQueue}.
 *
 * <p>Parallel workers add deferred writes without contention; the main
 * thread drains them via {@link #drainWrites()} after the parallel phase.</p>
 */
public final class ConcurrentWriteQueue implements WriteQueue {

    public static final ConcurrentWriteQueue INSTANCE = new ConcurrentWriteQueue();

    private final ConcurrentLinkedQueue<Runnable> deferred = new ConcurrentLinkedQueue<>();

    private ConcurrentWriteQueue() {}

    @Override
    public void addDeferred(Runnable write) {
        deferred.add(write);
    }

    @Override
    public void drainWrites() {
        Runnable r;
        while ((r = deferred.poll()) != null) r.run();
    }

    /** Clear all pending writes. Intended for test teardown only. */
    public static void resetForTesting() {
        INSTANCE.deferred.clear();
    }
}
