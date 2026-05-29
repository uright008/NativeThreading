package com.github.uright008.vec;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

public final class VectorialFabric implements PreLaunchEntrypoint {

    @Override
    public void onPreLaunch() {
        Vectorial.init();
    }
}
