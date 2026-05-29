package com.github.uright008.pc.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.github.uright008.pc.SafeLevelAccess;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.*;

/**
 * Protects iteration of {@code ServerLevel.navigatingMobs} during
 * {@code sendBlockUpdated} from concurrent modifications in safe zone.
 */
@Mixin(ServerLevel.class)
public abstract class ServerLevelSafeIterationMixin {

    @WrapOperation(
            method = "sendBlockUpdated",
            at = @At(value = "INVOKE", target = "Ljava/util/Set;iterator()Ljava/util/Iterator;")
    )
    private Iterator<Mob> safeIterator(Set<Mob> set, Operation<Iterator<Mob>> original) {
        if (!SafeLevelAccess.isInSafeZone()) {
            return original.call(set);
        }
        synchronized (set) {
            return new ArrayList<>(set).iterator();
        }
    }
}
