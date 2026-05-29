package com.github.uright008.em;

import net.fabricmc.api.ModInitializer;

public class EntityParallelizationFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        new EntityParallelization().onInitialize();
    }
}
