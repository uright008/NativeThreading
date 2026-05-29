package com.github.uright008.rp;

import net.fabricmc.api.ModInitializer;

public class RedstoneParallelizationFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        new RedstoneParallelization().onInitialize();
    }
}
