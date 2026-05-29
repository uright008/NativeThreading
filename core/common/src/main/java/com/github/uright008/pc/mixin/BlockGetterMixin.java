package com.github.uright008.pc.mixin;

import com.github.uright008.pc.util.BlockGetterPool;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.world.level.BlockGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(BlockGetter.class)
public interface BlockGetterMixin {

    @Redirect(method = "forEachBlockIntersectedBetween",
              at = @At(value = "NEW", target = "()Lit/unimi/dsi/fastutil/longs/LongOpenHashSet;"),
              require = 0)
    private static LongOpenHashSet reuseLongOpenHashSet() {
        return BlockGetterPool.get();
    }
}
