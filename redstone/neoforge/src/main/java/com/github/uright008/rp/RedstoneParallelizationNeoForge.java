package com.github.uright008.rp;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod("redstone")
public class RedstoneParallelizationNeoForge {
    public RedstoneParallelizationNeoForge(IEventBus modBus) {
        new RedstoneParallelization().onInitialize();
    }
}
