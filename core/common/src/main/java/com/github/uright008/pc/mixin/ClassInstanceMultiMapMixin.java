package com.github.uright008.pc.mixin;

import com.github.uright008.pc.SafeLevelAccess;
import net.minecraft.util.ClassInstanceMultiMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.*;

/**
 * Makes {@link ClassInstanceMultiMap} operations thread-safe when inside
 * the parallel safe zone ({@link SafeLevelAccess#isInSafeZone()}).
 *
 * <p>{@code ClassInstanceMultiMap.find()} uses {@link HashMap#computeIfAbsent}
 * which is not thread-safe.  When physics parallelization ticks entities in
 * parallel workers, entity AI's {@code getEntities()} calls corrupt the
 * internal {@code HashMap} and {@code ArrayList}, causing
 * {@code ConcurrentModificationException} and eventually cascading into
 * {@code NullPointerException} in unrelated map iterators.</p>
 *
 * <p>This mixin synchronizes write operations and returns copies for read
 * operations when the safe zone is active, following the same pattern as
 * {@link LevelSafeAccessMixin}.</p>
 */
@Mixin(ClassInstanceMultiMap.class)
public abstract class ClassInstanceMultiMapMixin<T> {

    @Shadow
    private Map<Class<?>, List<T>> byClass;

    @Shadow
    private Class<T> baseClass;

    @Shadow
    private List<T> allInstances;

    // ── find() : synchronize + return copy ────────
    @SuppressWarnings({"unchecked", "rawtypes"})
    @Inject(method = "find", at = @At("HEAD"), cancellable = true)
    private void onFind(Class index, CallbackInfoReturnable cir) {
        if (!SafeLevelAccess.isInSafeZone()) return;
        cir.setReturnValue(synchronizedFind(index));
    }

    @Unique
    private synchronized <S> Collection<S> synchronizedFind(Class<S> index) {
        if (!this.baseClass.isAssignableFrom(index)) {
            throw new IllegalArgumentException("Don't know how to search for " + index);
        }
        List<? extends T> instances = this.byClass.computeIfAbsent(
                index,
                k -> this.allInstances.stream()
                        .filter(k::isInstance)
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new))
        );
        return (Collection<S>) new ArrayList<>(instances);
    }

    // ── add() : synchronize ───────────────────────
    @SuppressWarnings("unchecked")
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void onAdd(Object instance, CallbackInfoReturnable<Boolean> cir) {
        if (!SafeLevelAccess.isInSafeZone()) return;
        cir.setReturnValue(synchronizedAdd((T) instance));
    }

    @Unique
    private synchronized boolean synchronizedAdd(T instance) {
        boolean success = false;
        for (Map.Entry<Class<?>, List<T>> entry : this.byClass.entrySet()) {
            if (entry.getKey().isInstance(instance)) {
                success |= entry.getValue().add(instance);
            }
        }
        return success;
    }

    // ── remove() : synchronize ────────────────────
    @Inject(method = "remove", at = @At("HEAD"), cancellable = true)
    private void onRemove(Object object, CallbackInfoReturnable<Boolean> cir) {
        if (!SafeLevelAccess.isInSafeZone()) return;
        cir.setReturnValue(synchronizedRemove(object));
    }

    @Unique
    private synchronized boolean synchronizedRemove(Object object) {
        boolean success = false;
        for (Map.Entry<Class<?>, List<T>> entry : this.byClass.entrySet()) {
            if (entry.getKey().isInstance(object)) {
                success |= entry.getValue().remove(object);
            }
        }
        return success;
    }

    // ── iterator() : return copy ──────────────────
    @SuppressWarnings("unchecked")
    @Inject(method = "iterator", at = @At("HEAD"), cancellable = true)
    private void onIterator(CallbackInfoReturnable<Iterator<T>> cir) {
        if (!SafeLevelAccess.isInSafeZone()) return;
        synchronized (this) {
            cir.setReturnValue(new ArrayList<>(this.allInstances).iterator());
        }
    }
}
