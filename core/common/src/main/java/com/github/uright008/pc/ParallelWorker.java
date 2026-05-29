package com.github.uright008.pc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Generic parallel dispatch: partitioning, latch, timeout, error collection,
 * auto safe-zone wrapping — all handled by core.
 *
 * <p>Subsystems provide only the task lambda; core manages everything else.</p>
 */
public final class ParallelWorker {

    private static final Logger LOG = LoggerFactory.getLogger("mc-parallel:worker");

    private ParallelWorker() {}

    /**
     * Execute a mapper function in parallel and return results in original order.
     * Workers are auto-wrapped with {@link SafeLevelAccess#runSafe}.
     *
     * @param executor       thread pool
     * @param items          input items
     * @param mapper         function applied to each item (inside safe zone)
     * @param timeoutSeconds latch timeout
     * @param <T>            input type
     * @param <R>            result type
     * @return list of results (may contain nulls for error slots)
     * @throws RuntimeException if workers time out or the latch is interrupted
     */
    public static <T, R> List<R> map(
            ExecutorService executor, List<T> items,
            Function<T, R> mapper, int timeoutSeconds) {

        int n = items.size();
        if (n == 0) return List.of();

        int workers = computeWorkers(n);
        if (workers == 1) {
            List<R> results = new ArrayList<>(n);
            SafeLevelAccess.runSafe(() -> {
                for (T item : items) results.add(mapper.apply(item));
            });
            SafeOps.drainWrites();
            return results;
        }

        R[] results = (R[]) new Object[n];
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        int perWorker = n / workers;
        int extra = n % workers;
        CountDownLatch latch = new CountDownLatch(workers);

        int offset = 0;
        for (int w = 0; w < workers; w++) {
            final int start = offset;
            final int end = offset + perWorker + (w < extra ? 1 : 0);
            offset = end;
            if (start >= end) {
                latch.countDown();
                continue;
            }
            executor.execute(() -> {
                SafeLevelAccess.runSafe(() -> {
                    try {
                        for (int i = start; i < end; i++) {
                            results[i] = mapper.apply(items.get(i));
                        }
                    } catch (Throwable t) {
                        firstError.compareAndSet(null, t);
                        LOG.error("Worker [{},{}) failed", start, end, t);
                    }
                });
                latch.countDown();
            });
        }

        awaitLatch(latch, timeoutSeconds);
        throwOnError(firstError.get());
        SafeOps.drainWrites();
        return Arrays.asList(results);
    }

    /**
     * Execute an action in parallel (void — for inline read+write tasks).
     * Workers are auto-wrapped with {@link SafeLevelAccess#runSafe}.
     *
     * @param executor       thread pool
     * @param items          input items
     * @param action         action performed on each item (inside safe zone, may write via {@link SafeOps})
     * @param timeoutSeconds latch timeout
     * @param <T>            input type
     * @throws RuntimeException if workers time out or the latch is interrupted
     */
    public static <T> void forEach(
            ExecutorService executor, List<T> items,
            Consumer<T> action, int timeoutSeconds) {

        int n = items.size();
        if (n == 0) return;

        int workers = computeWorkers(n);
        if (workers == 1) {
            SafeLevelAccess.runSafe(() -> {
                for (T item : items) action.accept(item);
            });
            SafeOps.drainWrites();
            return;
        }

        AtomicReference<Throwable> firstError = new AtomicReference<>();
        int perWorker = n / workers;
        int extra = n % workers;
        CountDownLatch latch = new CountDownLatch(workers);

        int offset = 0;
        for (int w = 0; w < workers; w++) {
            final int start = offset;
            final int end = offset + perWorker + (w < extra ? 1 : 0);
            offset = end;
            if (start >= end) {
                latch.countDown();
                continue;
            }
            executor.execute(() -> {
                SafeLevelAccess.runSafe(() -> {
                    try {
                        for (int i = start; i < end; i++) {
                            action.accept(items.get(i));
                        }
                    } catch (Throwable t) {
                        firstError.compareAndSet(null, t);
                        LOG.error("Worker [{},{}) failed", start, end, t);
                    }
                });
                latch.countDown();
            });
        }

        awaitLatch(latch, timeoutSeconds);
        throwOnError(firstError.get());
        SafeOps.drainWrites();
    }

    /**
     * Execute a mapper function in parallel, one worker per input item
     * (no further partitioning).  Use when each task is already a
     * pre-sized batch.
     */
    public static <T, R> List<R> mapEach(
            ExecutorService executor, List<T> tasks,
            Function<T, R> mapper, int timeoutSeconds) {

        int n = tasks.size();
        if (n == 0) return List.of();

        R[] results = (R[]) new Object[n];
        AtomicReference<Throwable> firstError = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(n);

        for (int i = 0; i < n; i++) {
            final int slot = i;
            executor.execute(() -> {
                SafeLevelAccess.runSafe(() -> {
                    try {
                        results[slot] = mapper.apply(tasks.get(slot));
                    } catch (Throwable t) {
                        firstError.compareAndSet(null, t);
                        LOG.error("Worker {} failed", slot, t);
                    }
                });
                latch.countDown();
            });
        }

        awaitLatch(latch, timeoutSeconds);
        throwOnError(firstError.get());
        return Arrays.asList(results);
    }

    static int computeWorkers(int itemCount) {
        int cpuCores = Runtime.getRuntime().availableProcessors();
        return Math.min(cpuCores, Math.max(2, itemCount / 16));
    }

    private static void throwOnError(Throwable err) {
        if (err != null) {
            if (err instanceof RuntimeException re) throw re;
            throw new RuntimeException("Worker failed", err);
        }
    }

    private static void awaitLatch(CountDownLatch latch, int timeoutSeconds) {
        boolean done;
        try {
            done = latch.await(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Workers interrupted", e);
        }
        if (!done) throw new RuntimeException("Workers timed out after " + timeoutSeconds + "s");
    }
}
