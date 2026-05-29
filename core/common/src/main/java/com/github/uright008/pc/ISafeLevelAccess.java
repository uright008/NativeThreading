package com.github.uright008.pc;

import java.util.function.Supplier;

/**
 * Thread-safe Level access abstraction.
 *
 * <p>Implementations manage per-thread re-entrant safe-zone depth so that
 * parallel workers can access vanilla Level methods that normally reject
 * off-main-thread calls.</p>
 */
public interface ISafeLevelAccess {

    void enterSafeZone();

    void leaveSafeZone();

    boolean isInSafeZone();

    void runSafe(Runnable action);

    <T> T runSafe(Supplier<T> action);

    /**
     * Returns the default ThreadLocal-based implementation.
     */
    static ISafeLevelAccess getDefault() {
        return ThreadLocalSafeLevelAccess.INSTANCE;
    }
}
