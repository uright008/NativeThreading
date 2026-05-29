package com.github.uright008.ep;

import net.fabricmc.api.ModInitializer;

public class ExplosionParallelizationFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        new ExplosionParallelization().onInitialize();
    }
}
