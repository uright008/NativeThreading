package com.github.uright008.hp;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * A pre-computed transfer intent produced by a hopper during the parallel
 * read-phase.  Plans are applied sequentially in the write-phase; a plan
 * that is no longer valid (target full, source empty, item already taken,
 * etc.) is simply skipped.
 *
 * <p>Two kinds of plan:</p>
 * <ul>
 *   <li><b>PUSH</b> — move one item from {@code hopperSlot} of this hopper
 *       into the container at {@code otherPos} (the hopper's facing direction).</li>
 *   <li><b>PULL</b> — move one item from the container at {@code otherPos}
 *       (above the hopper) into this hopper, OR pick up an ItemEntity.</li>
 * </ul>
 */
public final class HopperTransferPlan {

    public enum Kind { PUSH, PULL }

    final BlockPos hopperPos;
    final Kind kind;
    final BlockPos otherPos;
    final int hopperSlot;                     // relevant hopper slot (-1 if N/A)
    final ItemStack snapshot;                 // snapshot of the item at decision time
    final @Nullable Direction direction;      // facing for PUSH, DOWN for PULL container

    /** PUSH plan: move item from hopper slot → facing container. */
    static HopperTransferPlan push(BlockPos hopperPos, int slot, ItemStack snapshot,
                                   BlockPos targetPos, Direction facing) {
        return new HopperTransferPlan(hopperPos, Kind.PUSH, targetPos, slot, snapshot.copy(), facing);
    }

    /** PULL plan: move item from above container → hopper. */
    static HopperTransferPlan pullFromContainer(BlockPos hopperPos, BlockPos sourcePos,
                                                int sourceSlot, ItemStack snapshot) {
        return new HopperTransferPlan(hopperPos, Kind.PULL, sourcePos, sourceSlot, snapshot.copy(), Direction.DOWN);
    }

    /** PULL plan: pick up an ItemEntity.  {@code otherPos} = entity block-position. */
    static HopperTransferPlan pullEntity(BlockPos hopperPos, BlockPos entityPos, ItemStack snapshot) {
        return new HopperTransferPlan(hopperPos, Kind.PULL, entityPos, -1, snapshot.copy(), null);
    }

    private HopperTransferPlan(BlockPos hopperPos, Kind kind, BlockPos otherPos,
                               int hopperSlot, ItemStack snapshot,
                               @Nullable Direction direction) {
        this.hopperPos = hopperPos;
        this.kind = kind;
        this.otherPos = otherPos;
        this.hopperSlot = hopperSlot;
        this.snapshot = snapshot;
        this.direction = direction;
    }
}
