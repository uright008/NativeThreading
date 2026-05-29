package com.github.uright008.pc.mixin;

import com.github.uright008.pc.SafeLevelAccess;
import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.entity.EntitySection;
import net.minecraft.world.level.entity.EntitySectionStorage;
import net.minecraft.world.level.entity.Visibility;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Protects {@link EntitySectionStorage} from concurrent mutations during
 * parallel entity ticks by synchronising on the storage instance when the
 * safe zone is active.
 */
@Mixin(EntitySectionStorage.class)
public abstract class EntitySectionStorageMixin {

    @Shadow
    private Long2ObjectMap<EntitySection<?>> sections;

    @Shadow
    private LongSortedSet sectionIds;

    @SuppressWarnings("rawtypes")
    @Shadow
    private Class entityClass;

    @Shadow
    private Long2ObjectFunction<Visibility> intialSectionVisibility;

    // ── getOrCreateSection : synchronise ───────────

    @SuppressWarnings("unchecked")
    @Inject(method = "getOrCreateSection", at = @At("HEAD"), cancellable = true)
    private void onGetOrCreateSection(long key, CallbackInfoReturnable<EntitySection<?>> cir) {
        if (!SafeLevelAccess.isInSafeZone()) return;
        synchronized (this) {
            EntitySection<?> existing = this.sections.get(key);
            if (existing != null) {
                cir.setReturnValue(existing);
                return;
            }
            long chunkPos = parallelCore$chunkKey(SectionPos.x(key), SectionPos.z(key));
            Visibility status = this.intialSectionVisibility.get(chunkPos);
            this.sectionIds.add(key);
            EntitySection<?> section = new EntitySection<>(this.entityClass, status);
            this.sections.put(key, section);
            cir.setReturnValue(section);
        }
    }

    @Unique
    private static long parallelCore$chunkKey(int x, int z) {
        return ((long) x & 0xffffffffL) | (((long) z & 0xffffffffL) << 32);
    }

    // ── remove : synchronise ───────────────────────

    @SuppressWarnings("unchecked")
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void onRemove(long sectionKey, CallbackInfo ci) {
        if (!SafeLevelAccess.isInSafeZone()) return;
        synchronized (this) {
            this.sections.remove(sectionKey);
            this.sectionIds.remove(sectionKey);
        }
        ci.cancel();
    }
}
