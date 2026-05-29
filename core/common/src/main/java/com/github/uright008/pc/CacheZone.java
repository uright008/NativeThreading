package com.github.uright008.pc;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

public final class CacheZone {

    private static final ThreadLocal<Long2ObjectOpenHashMap<BlockState>> holder = new ThreadLocal<>();

    private CacheZone() {}

    public static void enter() {
        holder.set(new Long2ObjectOpenHashMap<>());
    }

    public static void exit() {
        holder.remove();
    }

    public static boolean isActive() {
        return holder.get() != null;
    }

    public static BlockState get(BlockPos pos) {
        var map = holder.get();
        return map != null ? map.get(pos.asLong()) : null;
    }

    public static void put(BlockPos pos, BlockState state) {
        var map = holder.get();
        if (map != null) map.put(pos.asLong(), state);
    }
}
