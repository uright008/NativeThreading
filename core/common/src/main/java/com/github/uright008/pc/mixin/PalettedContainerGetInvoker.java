package com.github.uright008.pc.mixin;

import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(PalettedContainer.class)
public interface PalettedContainerGetInvoker {

    @Invoker("get")
    <T> T invokeGet(int index);
}
