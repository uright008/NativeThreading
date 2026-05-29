package com.github.uright008.pc.mixin;

import com.github.uright008.pc.ThreadSafeRandomSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Replaces every {@link Level}'s {@code RandomSource} with a thread-safe
 * implementation at construction time.
 *
 * <p>Vanilla {@code LegacyRandomSource} deliberately throws on concurrent
 * access ({@code compareAndSet} failure).  During parallel entity ticks
 * AI behaviours call {@code level.getRandom()} which triggers
 * {@code IllegalStateException: Accessing LegacyRandomSource from multiple
 * threads}.  {@link ThreadSafeRandomSource} uses the identical LCG
 * algorithm but retries on CAS failure instead of throwing.</p>
 */
@Mixin(Level.class)
public abstract class LevelRandomMixin {

    @Mutable
    @Shadow
    @Final
    private RandomSource random;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void replaceRandom(CallbackInfo ci) {
        this.random = ThreadSafeRandomSource.of(this.random);
    }
}
