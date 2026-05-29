package com.github.uright008.vec.mixin;

import com.github.uright008.vec.Vectorial;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftServer.class)
public class IgniteVectorialInit {
    @Inject(method = "runServer", at = @At("HEAD"))
    private void onServerStart(CallbackInfo ci) {
        Vectorial.init();
    }
}
