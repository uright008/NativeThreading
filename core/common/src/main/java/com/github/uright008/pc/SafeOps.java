package com.github.uright008.pc;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.ticks.TickPriority;

/**
 * Thread-safe world writes via deferred execution.
 *
 * <p>Workers inside the safe zone call write primitives, which record the
 * operation into a {@link WriteQueue} rather than executing it immediately.
 * After workers complete, the main thread calls {@link #drainWrites()} to
 * apply all mutations sequentially — eliminating lock contention on shared
 * world objects.</p>
 */
public final class SafeOps {

    private static final WriteQueue queue = ConcurrentWriteQueue.INSTANCE;

    private SafeOps() {}

    // ── Deferred writes ────────────────────────

    public static void setBlock(ServerLevel level, BlockPos pos, BlockState state, int flags) {
        queue.addDeferred(() -> level.setBlock(pos, state, flags));
    }

    public static void scheduleTick(ServerLevel level, BlockPos pos, Block block, int delay, TickPriority priority) {
        queue.addDeferred(() -> level.scheduleTick(pos, block, delay, priority));
    }

    // ── Thread-safe immediate writes (containers) ──

    public static ItemStack removeItem(Container container, int slot, int count) {
        synchronized (container) {
            return container.removeItem(slot, count);
        }
    }

    public static void setItem(Container container, int slot, ItemStack stack) {
        synchronized (container) {
            container.setItem(slot, stack);
        }
    }

    public static void setChanged(Container container) {
        synchronized (container) {
            container.setChanged();
        }
    }

    public static ItemStack addItem(Container source, Container target, ItemStack stack,
                                     net.minecraft.core.Direction side) {
        synchronized (target) {
            return HopperBlockEntity.addItem(source, target, stack, side);
        }
    }

    public static boolean addItem(Container hopper, net.minecraft.world.entity.item.ItemEntity entity) {
        synchronized (hopper) {
            return HopperBlockEntity.addItem(hopper, entity);
        }
    }

    // ── Drain ──────────────────────────────────

    /** Drain all pending deferred writes. Must be called on the main thread. */
    public static void drainWrites() {
        queue.drainWrites();
    }

    /** Clear all pending writes. Intended for test teardown only. */
    public static void resetForTesting() {
        ConcurrentWriteQueue.resetForTesting();
    }
}
