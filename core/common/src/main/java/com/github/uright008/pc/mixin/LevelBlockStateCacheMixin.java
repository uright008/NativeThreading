package com.github.uright008.pc.mixin;

import com.github.uright008.pc.CacheZone;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Level.class)
public abstract class LevelBlockStateCacheMixin {

    @Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
    private void onGetBlockState(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (!CacheZone.isActive()) return;
        BlockState cached = CacheZone.get(pos);
        if (cached != null) {
            cir.setReturnValue(cached);
        }
    }

    @Inject(method = "getBlockState", at = @At("RETURN"))
    private void onGetBlockStateReturn(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
        if (!CacheZone.isActive()) return;
        BlockState result = cir.getReturnValue();
        if (result != null) {
            CacheZone.put(pos, result);
        }
    }
}
