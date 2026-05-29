package com.github.uright008.hp;

import com.github.uright008.pc.command.ParallelCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HopperParallelization {
    public static final String MOD_ID = "hopper";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public void onInitialize() {
        LOGGER.info("Hopper Parallelization initializing...");
        HopperParallelConfig.init();
        ParallelCommand.registerSubCommand(new HopperParallelCommand());
        LOGGER.info("Hopper Parallelization ready — use /parallel hopper");
    }
}
