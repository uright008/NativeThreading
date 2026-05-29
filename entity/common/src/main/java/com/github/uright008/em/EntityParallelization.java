package com.github.uright008.em;

import com.github.uright008.pc.command.ParallelCommand;

public class EntityParallelization {
    public void onInitialize() {
        EntityParallelConfig.init();
        ParallelCommand.registerSubCommand(new EntityParallelCommand());
    }
}
