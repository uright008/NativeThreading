package com.github.uright008.pc;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod("parallel-core")
public class ParallelCoreNeoForge {
    public ParallelCoreNeoForge(IEventBus modBus) {
        new ParallelCore().onInitialize();
    }
}
