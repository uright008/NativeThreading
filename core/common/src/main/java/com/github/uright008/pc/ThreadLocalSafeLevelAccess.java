package com.github.uright008.pc;

import java.util.function.Supplier;

/**
 * ThreadLocal-based implementation of {@link ISafeLevelAccess}.
 *
 * <p>Uses a per-thread re-entrant depth counter so that nested enter/leave
 * pairs remain balanced. Mixins check {@link #isInSafeZone()} to decide
 * whether to use synchronized/redirected access to vanilla data structures.</p>
 */
public final class ThreadLocalSafeLevelAccess implements ISafeLevelAccess {

    public static final ThreadLocalSafeLevelAccess INSTANCE = new ThreadLocalSafeLevelAccess();

    private final ThreadLocal<Integer> safeZoneDepth = new ThreadLocal<>();

    private ThreadLocalSafeLevelAccess() {}

    @Override
    public void enterSafeZone() {
        Integer depth = safeZoneDepth.get();
        safeZoneDepth.set(depth == null ? 1 : depth + 1);
    }

    @Override
    public void leaveSafeZone() {
        Integer depth = safeZoneDepth.get();
        if (depth == null || depth <= 1) {
            safeZoneDepth.remove();
            return;
        }
        safeZoneDepth.set(depth - 1);
    }

    @Override
    public boolean isInSafeZone() {
        Integer depth = safeZoneDepth.get();
        return depth != null && depth > 0;
    }

    @Override
    public void runSafe(Runnable action) {
        enterSafeZone();
        try {
            action.run();
        } finally {
            leaveSafeZone();
        }
    }

    @Override
    public <T> T runSafe(Supplier<T> action) {
        enterSafeZone();
        try {
            return action.get();
        } finally {
            leaveSafeZone();
        }
    }

    /**
     * Clears the ThreadLocal state for the current thread. Intended for test cleanup.
     */
    public static void resetForTesting() {
        INSTANCE.safeZoneDepth.remove();
    }
}
