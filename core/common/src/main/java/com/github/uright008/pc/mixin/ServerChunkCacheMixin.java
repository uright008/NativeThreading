package com.github.uright008.pc.mixin;

import com.github.uright008.pc.ChunkSafeAccessor;
import com.github.uright008.pc.SafeLevelAccess;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Thread-safe chunk access for worker threads.
 */
@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin implements ChunkSafeAccessor {

    @Shadow
    @Nullable
    private native ChunkHolder getVisibleChunkIfPresent(long key);

    @Override
    @Unique
    @Nullable
    public ChunkAccess parallelCore$getChunkSafe(int x, int z) {
        long key = parallelCore$chunkKey(x, z);
        ChunkHolder holder = getVisibleChunkIfPresent(key);
        if (holder == null) return null;
        return holder.getChunkIfPresentUnchecked(ChunkStatus.FULL);
    }

    @Unique
    private static long parallelCore$chunkKey(int x, int z) {
        return ((long) x & 0xffffffffL) | (((long) z & 0xffffffffL) << 32);
    }

    /**
     * Intercept the vanilla {@code getChunk(int,int,ChunkStatus,boolean)}
     * which dispatches to the main thread via {@code CompletableFuture}.
     * When inside the safe zone, serve from {@link #parallelCore$getChunkSafe}
     * instead to avoid deadlocking the main thread.
     *
     * <p>Targeting the 4-arg overload because that is the actual method that
     * contains the main-thread dispatch logic.  {@code Level.getChunkForCollisions}
     * calls {@code getChunk(x, z, ChunkStatus.FULL, false)} which is this method.</p>
     */
    @Inject(method = "getChunk(IILnet/minecraft/world/level/chunk/status/ChunkStatus;Z)"
            + "Lnet/minecraft/world/level/chunk/ChunkAccess;",
            at = @At("HEAD"), cancellable = true)
    private void onGetChunk(int x, int z, ChunkStatus status, boolean loadOrGenerate,
                           CallbackInfoReturnable<@Nullable ChunkAccess> cir) {
        if (!SafeLevelAccess.isInSafeZone()) return;
        ChunkAccess ca = parallelCore$getChunkSafe(x, z);
        if (ca != null) {
            cir.setReturnValue(ca);
        }
    }
}
