package com.github.uright008.pc.mixin;

import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes the protected {@code PalettedContainer.get(int)} method so that
 * {@link com.github.uright008.pc.SectionBlockReader} can call it directly,
 * bypassing the public {@code get(x,y,z)} wrapper and {@code getIndex()}.
 */
@Mixin(PalettedContainer.class)
public interface PalettedContainerGetInvoker {

    @Invoker("get")
    <T> T invokeGet(int index);
}
