package com.github.uright008.pc;

import com.github.uright008.pc.mixin.PalettedContainerFastAccessMixin;
import net.minecraft.util.BitStorage;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.Palette;
import net.minecraft.world.level.chunk.PalettedContainer;

/**
 * Zero-allocation fast block-state lookup for hot loops.
 *
 * <p>Captures a section's {@link BitStorage} and {@link Palette} <em>once</em>
 * per section change, then uses direct index-based access:</p>
 * <pre>{@code
 *   BitStorage stg = FastBlockAccess.storage(section);
 *   Palette<BlockState> pal = FastBlockAccess.palette(section);
 *   for (...) {
 *       int idx = (y & 15) << 8 | (z & 15) << 4 | (x & 15);
 *       BlockState state = pal.valueFor(stg.get(idx));
 *   }
 * }</pre>
 *
 * <p>Bypasses {@code LevelChunkSection.getBlockState()},
 * {@code PalettedContainer.get(x,y,z)}, {@code Strategy.getIndex()},
 * and the volatile {@code data} field read on every call.</p>
 */
public final class FastBlockAccess {

    private FastBlockAccess() {}

    @SuppressWarnings("unchecked")
    public static BitStorage storage(LevelChunkSection section) {
        PalettedContainer<BlockState> container = section.getStates();
        return ((PalettedContainerFastAccessMixin)(Object)container).parallelCore$getStorage();
    }

    @SuppressWarnings("unchecked")
    public static Palette<BlockState> palette(LevelChunkSection section) {
        PalettedContainer<BlockState> container = section.getStates();
        return (Palette<BlockState>)(Object)
                ((PalettedContainerFastAccessMixin)(Object)container).parallelCore$getPalette();
    }
}
