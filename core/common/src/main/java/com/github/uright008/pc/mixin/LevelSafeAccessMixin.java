package com.github.uright008.pc.mixin;

import com.github.uright008.pc.ChunkSafeAccessor;
import com.github.uright008.pc.SafeLevelAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes chunk-based Level lookups safe for worker threads.
 *
 * <p>Two strategies are combined:</p>
 * <ol>
 *   <li>{@link Redirect} the {@code Thread.currentThread()} call inside
 *       {@code getBlockEntity()} — pretends we are the main thread so the
 *       thread check passes.</li>
 *   <li>{@link Inject} the {@code getChunk(int,int)} overload — serves
 *       chunks via {@link ChunkSafeAccessor} to avoid dispatching to the
 *       main thread (which would deadlock).</li>
 * </ol>
 *
 * <p>All vanilla logic (bounds checks, block state lookups, fluid state, etc.)
 * runs exactly as in vanilla.</p>
 */
@Mixin(Level.class)
public abstract class LevelSafeAccessMixin {

    @Shadow
    private Thread thread;

    // ── getBlockEntity: bypass thread check ──────

    /**
     * Vanilla {@code getBlockEntity()} does
     * {@code Thread.currentThread() != this.thread ? null : ...}.
     * When inside the safe zone we pretend the current thread <em>is</em> the
     * main thread so the real lookup path runs.
     */
    @Redirect(method = "getBlockEntity",
              at = @At(value = "INVOKE", target = "Ljava/lang/Thread;currentThread()Ljava/lang/Thread;"))
    private Thread redirectCurrentThread() {
        if (SafeLevelAccess.isInSafeZone()) {
            return this.thread;
        }
        return Thread.currentThread();
    }

    // ── getChunk: use ChunkSafeAccessor ──────────

    /**
     * Intercept the 2-arg {@code getChunk(int,int)} returning {@link LevelChunk}.
     * Used by {@code getBlockState()}, {@code getChunkAt()}, {@code getFluidState()},
     * and most other position-based lookups.
     *
     * <p>The 4-arg overload ({@code getChunk(int,int,ChunkStatus,boolean)}) is
     * not intercepted because its signature changed across Minecraft versions.
     * The 2-arg version is the primary path for block-state and block-entity
     * queries and is sufficient for worker-thread safety.</p>
     */
    @Inject(method = "getChunk(II)Lnet/minecraft/world/level/chunk/LevelChunk;",
            at = @At("HEAD"), cancellable = true)
    private void onGetChunk2(int chunkX, int chunkZ, CallbackInfoReturnable<LevelChunk> cir) {
        if (!SafeLevelAccess.isInSafeZone()) return;
        Level self = (Level) (Object) this;
        if (self.getChunkSource() instanceof ChunkSafeAccessor scs) {
            ChunkAccess ca = scs.parallelCore$getChunkSafe(chunkX, chunkZ);
            if (ca instanceof LevelChunk lc) {
                cir.setReturnValue(lc);
            }
        }
    }

    /**
     * Intercept {@code getChunkForCollisions(int,int)} which is called by
     * {@code BlockCollisions} during block collision resolution.  When in
     * the safe zone, serve chunks from {@link ChunkSafeAccessor} to avoid
     * dispatching to the main thread.
     */
    @Inject(method = "getChunkForCollisions(II)Lnet/minecraft/world/level/BlockGetter;",
            at = @At("HEAD"), cancellable = true)
    private void onGetChunkForCollisions(int chunkX, int chunkZ, CallbackInfoReturnable<BlockGetter> cir) {
        if (!SafeLevelAccess.isInSafeZone()) return;
        Level self = (Level) (Object) this;
        if (self.getChunkSource() instanceof ChunkSafeAccessor scs) {
            ChunkAccess ca = scs.parallelCore$getChunkSafe(chunkX, chunkZ);
            if (ca instanceof BlockGetter bg) {
                cir.setReturnValue(bg);
            }
        }
    }
}
