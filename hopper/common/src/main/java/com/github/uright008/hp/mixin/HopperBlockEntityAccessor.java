package com.github.uright008.hp.mixin;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

/**
 * Exposes private fields and methods of {@link HopperBlockEntity}
 * so the parallel helper can read/write cooldown and check inventory state.
 */
@Mixin(HopperBlockEntity.class)
public interface HopperBlockEntityAccessor {

    @Accessor("cooldownTime")
    int getCooldownTime();

    @Accessor("cooldownTime")
    void setCooldownTime(int time);

    @Accessor("facing")
    Direction getFacing();

    @Invoker("inventoryFull")
    boolean invokeInventoryFull();

    @Accessor("tickedGameTime")
    void setTickedGameTime(long time);

}
