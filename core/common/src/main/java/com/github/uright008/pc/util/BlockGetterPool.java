package com.github.uright008.pc.util;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;

/**
 * ThreadLocal pool of LongOpenHashSet for reuse.
 * Eliminates the 4MB-per-tick allocation in forEachBlockIntersectedBetween.
 */
public final class BlockGetterPool {
    private static final ThreadLocal<LongOpenHashSet> POOL =
            ThreadLocal.withInitial(() -> {
                LongOpenHashSet s = new LongOpenHashSet();
                s.clear();
                return s;
            });

    public static LongOpenHashSet get() {
        LongOpenHashSet s = POOL.get();
        s.clear();
        return s;
    }

    public static void resetForTesting() {
        POOL.remove();
    }
}
