package com.github.uright008.pc;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

/**
 * Immutable snapshot of an {@link net.minecraft.world.entity.item.ItemEntity}.
 * Captured on the main thread before a parallel phase so worker threads
 * can safely read item data without touching the live entity.
 */
public record ItemEntitySnapshot(ItemStack item, BlockPos pos) {}
