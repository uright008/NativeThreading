package com.github.uright008.pc;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import java.lang.reflect.Method;

public final class SectionBlockReader {

    private final PalettedContainer<BlockState> container;

    private SectionBlockReader(PalettedContainer<BlockState> container) {
        this.container = container;
    }

    private static final Method GET;

    static {
        try {
            GET = PalettedContainer.class.getDeclaredMethod("get", int.class);
            GET.setAccessible(true);
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
            return (BlockState) GET.invoke(container, idx);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
