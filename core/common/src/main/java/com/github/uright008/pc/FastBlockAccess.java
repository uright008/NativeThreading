package com.github.uright008.pc;

import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.VarHandle;

public final class FastBlockAccess {

    private FastBlockAccess() {}

    private static final VarHandle DATA_FIELD;
    private static final MethodHandle DATA_STORAGE;
    private static final MethodHandle DATA_PALETTE;

    static {
        try {
            Class<?> containerClass = PalettedContainer.class;
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

    public static BitStorage storage(LevelChunkSection section) {
        try {
            return (BitStorage) DATA_STORAGE.invoke(DATA_FIELD.get(section.getStates()));
        } catch (RuntimeException | Error e) { throw e; }
        catch (Throwable e) { throw new RuntimeException(e); }
    }

    @SuppressWarnings("unchecked")
    public static Palette<BlockState> palette(LevelChunkSection section) {
        try {
            Object data = DATA_FIELD.get(section.getStates());
            return (Palette<BlockState>)(Object) DATA_PALETTE.invoke(data);
        } catch (RuntimeException | Error e) { throw e; }
        catch (Throwable e) { throw new RuntimeException(e); }
    }
}
