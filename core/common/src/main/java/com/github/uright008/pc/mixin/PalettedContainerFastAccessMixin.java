package com.github.uright008.pc.mixin;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

/**
 * Exposes {@code PalettedContainer.Data} components ({@link BitStorage} + {@link Palette})
 * via cached {@link VarHandle}/{@link MethodHandle}, bypassing private record visibility
 * and obfuscation.
 *
 * <p>Callers capture storage + palette once per section change, then use
 * {@code palette.valueFor(storage.get(index))} directly in hot loops.</p>
 */
@Mixin(PalettedContainer.class)
public abstract class PalettedContainerFastAccessMixin {

    private static final VarHandle DATA_FIELD;
    private static final MethodHandle DATA_STORAGE;
    private static final MethodHandle DATA_PALETTE;

    static {
        try {
            Class<?> containerClass = PalettedContainer.class;
            // The "data" field is the only field whose type is an inner record of PalettedContainer.
            // Search by this heuristic to handle obfuscation.
            VarHandle vh = null;
            for (var f : containerClass.getDeclaredFields()) {
                if (f.getType().getEnclosingClass() == containerClass) {
                    vh = MethodHandles.privateLookupIn(containerClass, MethodHandles.lookup())
                            .unreflectVarHandle(f);
                    break;
                }
            }
            if (vh == null) throw new NoSuchFieldError("PalettedContainer 'data' field not found");
            DATA_FIELD = vh;

            Class<?> dataClass = DATA_FIELD.varType();
            var lookup = MethodHandles.privateLookupIn(dataClass, MethodHandles.lookup());
            DATA_STORAGE = lookup.findVirtual(dataClass, "storage", MethodType.methodType(BitStorage.class));
            DATA_PALETTE = lookup.findVirtual(dataClass, "palette", MethodType.methodType(Palette.class));
        } catch (Throwable e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Unique
    public BitStorage parallelCore$getStorage() {
        try {
            return (BitStorage) DATA_STORAGE.invoke(DATA_FIELD.get(this));
        } catch (RuntimeException | Error e) { throw e; }
        catch (Throwable e) { throw new RuntimeException(e); }
    }

    @SuppressWarnings("unchecked")
    @Unique
    public <T> Palette<T> parallelCore$getPalette() {
        try {
            return (Palette<T>) DATA_PALETTE.invoke(DATA_FIELD.get(this));
        } catch (RuntimeException | Error e) { throw e; }
        catch (Throwable e) { throw new RuntimeException(e); }
    }
}
