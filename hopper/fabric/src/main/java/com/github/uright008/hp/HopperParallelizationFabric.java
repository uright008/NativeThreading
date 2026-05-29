package com.github.uright008.hp;

import net.fabricmc.api.ModInitializer;

public class HopperParallelizationFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        new HopperParallelization().onInitialize();
    }
}
