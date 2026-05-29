package com.github.uright008.pc;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Thread-safe block entity access for worker threads.
 * Uses {@link ChunkSafeAccessor} to read chunks directly without
 * dispatching to the main thread.
 */
public final class BlockEntityHelper {

    private BlockEntityHelper() {}

    /**
     * Get a block entity from the given level, safe for worker threads.
     * Uses ChunkSafeAccessor to avoid main-thread dispatch.
     */
    @Nullable
    public static BlockEntity getBlockEntitySafe(Level level, BlockPos pos) {
        if (!level.isInWorldBounds(pos)) return null;

        int cx = SectionPos.blockToSectionCoord(pos.getX());
        int cz = SectionPos.blockToSectionCoord(pos.getZ());

        if (level.getChunkSource() instanceof ChunkSafeAccessor scs) {
            ChunkAccess chunk = scs.parallelCore$getChunkSafe(cx, cz);
            if (chunk != null) {
                return chunk.getBlockEntity(pos);
            }
        }
        return null;
    }

    /**
     * Pre-load chunks around a position on the main thread.
     * Must be called from the main thread before parallel processing.
     */
    public static void ensureChunkLoaded(Level level, BlockPos pos) {
        int cx = SectionPos.blockToSectionCoord(pos.getX());
        int cz = SectionPos.blockToSectionCoord(pos.getZ());
        level.getChunk(cx, cz);
    }

    /**
     * Pre-load chunks in a radius around a position.
     */
    public static void ensureChunksLoaded(Level level, BlockPos center, int radius) {
        int cx = SectionPos.blockToSectionCoord(center.getX());
        int cz = SectionPos.blockToSectionCoord(center.getZ());
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                level.getChunk(cx + dx, cz + dz);
            }
        }
    }
}
