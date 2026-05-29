package com.github.uright008.rp;

import com.github.uright008.pc.command.ParallelCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedstoneParallelization {
    public static final String MOD_ID = "redstone";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public void onInitialize() {
        LOGGER.info("Redstone Parallelization initializing...");
        RedstoneParallelConfig.init();
        ParallelCommand.registerSubCommand(new RedstoneParallelCommand());
        LOGGER.info("Redstone Parallelization ready — use /parallel redstone");
    }
}
