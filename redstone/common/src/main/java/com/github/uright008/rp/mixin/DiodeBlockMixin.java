package com.github.uright008.rp.mixin;

import com.github.uright008.rp.DiodeTickBatcher;
import com.github.uright008.rp.RedstoneParallelConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DiodeBlock.class)
public abstract class DiodeBlockMixin {

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void onTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random, CallbackInfo ci) {
        if (!RedstoneParallelConfig.isDiodeEnabled()) return;
        if (!(state.getBlock() instanceof net.minecraft.world.level.block.RepeaterBlock)) return;
        DiodeTickBatcher.add(level, pos, state);
        ci.cancel();
    }
}
