package com.github.uright008.pc;

import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;

/**
 * Interface that ServerChunkCacheMixin injects into ServerChunkCache.
 * Allows type-safe cross-mixin access to the thread-safe chunk reader.
 *
 * Moved from explosion-parallelization into parallel-core as part of
 * the world-management layer.
 */
public interface ChunkSafeAccessor {
    @Nullable
    ChunkAccess parallelCore$getChunkSafe(int x, int z);
}
