package com.github.uright008.pc;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public final class SectionBlockReader {

    private final PalettedContainer<BlockState> container;

    private SectionBlockReader(PalettedContainer<BlockState> container) {
        this.container = container;
    }

    private static final MethodHandle GET;

    static {
        try {
            var lookup = MethodHandles.privateLookupIn(
                    PalettedContainer.class, MethodHandles.lookup());
            GET = lookup.findVirtual(PalettedContainer.class, "get",
                    MethodType.methodType(Object.class, int.class));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static SectionBlockReader of(LevelChunkSection section) {
        PalettedContainer<BlockState> c = section.getStates();
        return new SectionBlockReader(c);
    }

    @SuppressWarnings("unchecked")
    public BlockState get(int x, int y, int z) {
        int idx = (y & 15) << 8 | (z & 15) << 4 | (x & 15);
        try {
            Object result = GET.invokeExact(container, idx);
            return (BlockState) result;
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }
}
