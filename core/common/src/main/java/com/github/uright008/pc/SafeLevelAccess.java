package com.github.uright008.pc;

import java.util.function.Supplier;

/**
 * Thread-safe Level access via {@link ChunkSafeAccessor}.
 *
 * <p>Vanilla {@code Level.getBlockEntity()} returns null when called from
 * a non-main thread. Each worker thread must call {@link #enterSafeZone()}
 * before its own world access and {@link #leaveSafeZone()} afterwards.
 * Mixins in this mod intercept {@code getBlockEntity} / {@code getBlockState}
 * and use {@link ChunkSafeAccessor} when inside the safe zone.</p>
 *
 * <p>Uses per-thread re-entrant state so only the current thread sees the
 * safe zone after entering it, and nested enter/leave pairs remain balanced.</p>
 *
 * <p>Convenience wrappers {@link #runSafe(Runnable)} and {@link #runSafe(Supplier)}
 * eliminate the enter/try/finally/leave boilerplate.</p>
 */
public final class SafeLevelAccess {

    private static final ThreadLocal<Integer> safeZoneDepth = new ThreadLocal<>();

    private SafeLevelAccess() {}

    public static void enterSafeZone() {
        Integer depth = safeZoneDepth.get();
        safeZoneDepth.set(depth == null ? 1 : depth + 1);
    }

    public static void leaveSafeZone() {
        Integer depth = safeZoneDepth.get();
        if (depth == null || depth <= 1) {
            safeZoneDepth.remove();
            return;
        }
        safeZoneDepth.set(depth - 1);
    }

    public static boolean isInSafeZone() {
        Integer depth = safeZoneDepth.get();
        return depth != null && depth > 0;
    }

    public static void runSafe(Runnable action) {
        enterSafeZone();
        try { action.run(); }
        finally { leaveSafeZone(); }
    }

    public static <T> T runSafe(Supplier<T> action) {
        enterSafeZone();
        try { return action.get(); }
        finally { leaveSafeZone(); }
    }
}
