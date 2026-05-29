package com.github.uright008.ep;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod("explosion")
public class ExplosionParallelizationNeoForge {
    public ExplosionParallelizationNeoForge(IEventBus modBus) {
        new ExplosionParallelization().onInitialize();
    }
}
