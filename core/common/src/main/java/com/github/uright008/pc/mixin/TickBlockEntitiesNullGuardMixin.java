package com.github.uright008.pc.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Guards {@code shouldTickBlocksAt(BlockPos)} against null positions.
 *
 * <p>When parallel subsystems run worker threads that interact with chunks,
 * a {@code TickingBlockEntity} can have its position set to null during
 * the iterator loop in {@code tickBlockEntities()}, causing an NPE in
 * {@code ChunkPos.pack(pos)}.  Returning {@code false} is safe — the
 * block entity simply skips this tick.</p>
 */
@Mixin(Level.class)
public abstract class TickBlockEntitiesNullGuardMixin {

    @Inject(method = "shouldTickBlocksAt(Lnet/minecraft/core/BlockPos;)Z",
            at = @At("HEAD"), cancellable = true)
    private void guardNullPos(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (pos == null) {
            cir.setReturnValue(false);
        }
    }
}
