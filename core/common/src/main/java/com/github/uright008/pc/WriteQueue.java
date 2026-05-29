package com.github.uright008.pc;

/**
 * Abstraction for the deferred write queue used by {@link SafeOps}.
 *
 * <p>The default implementation ({@link ConcurrentWriteQueue}) uses a
 * {@link java.util.concurrent.ConcurrentLinkedQueue} so that parallel
 * workers can enqueue writes without locking, while the main thread
 * drains them sequentially after the parallel phase completes.</p>
 */
public interface WriteQueue {

    /** Enqueue a write operation for later execution on the main thread. */
    void addDeferred(Runnable write);

    /** Drain and execute all pending deferred writes. Must be called on the main thread. */
    void drainWrites();

    /** Returns the default production implementation. */
    static WriteQueue getDefault() { return ConcurrentWriteQueue.INSTANCE; }
}
