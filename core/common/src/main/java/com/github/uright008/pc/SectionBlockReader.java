package com.github.uright008.pc;

import com.github.uright008.pc.mixin.PalettedContainerGetInvoker;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;

/**
 * Fast block-state reader that bypasses {@code LevelChunkSection.getBlockState()}
 * and {@code PalettedContainer.get(x,y,z)} + {@code Strategy.getIndex()}.
 *
 * <p>Hot path: ONE virtual call to {@code PalettedContainer.get(int)} instead
 * of the original three-method chain.</p>
 *
 * <p>Thread-safe: each caller creates its own instance.</p>
 */
public final class SectionBlockReader {

    private final PalettedContainerGetInvoker invoker;

    private SectionBlockReader(PalettedContainerGetInvoker invoker) {
        this.invoker = invoker;
    }

    public static SectionBlockReader of(LevelChunkSection section) {
        PalettedContainer<BlockState> c = section.getStates();
        return new SectionBlockReader((PalettedContainerGetInvoker)(Object)c);
    }

    public BlockState get(int x, int y, int z) {
        int idx = (y & 15) << 8 | (z & 15) << 4 | (x & 15);
        return invoker.invokeGet(idx);
    }
}
