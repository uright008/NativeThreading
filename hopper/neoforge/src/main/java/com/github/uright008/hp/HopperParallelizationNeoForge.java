package com.github.uright008.hp;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

@Mod("hopper")
public class HopperParallelizationNeoForge {
    public HopperParallelizationNeoForge(IEventBus modBus) {
        new HopperParallelization().onInitialize();
    }
}
