package com.github.uright008.pc;

import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.ChunkAccess;
import org.jetbrains.annotations.Nullable;

public final class ChunkGrid {

    private final ChunkAccess[][] chunks;
    private final int minSectionX;
    private final int minSectionZ;
    private final int sizeX;
    private final int sizeZ;

    public ChunkGrid(ServerLevel level, double centerX, double centerZ, float radius) {
        int scx = SectionPos.blockToSectionCoord((int) Math.floor(centerX));
        int scz = SectionPos.blockToSectionCoord((int) Math.floor(centerZ));
        int range = (int) Math.ceil(radius / 16.0) + 1;
        this.sizeX = range * 2 + 1;
        this.sizeZ = range * 2 + 1;
        this.minSectionX = scx - range;
        this.minSectionZ = scz - range;
        this.chunks = new ChunkAccess[sizeX][sizeZ];

        ChunkSafeAccessor scs = (ChunkSafeAccessor) level.getChunkSource();
        for (int dx = 0; dx < sizeX; dx++) {
            for (int dz = 0; dz < sizeZ; dz++) {
                this.chunks[dx][dz] = scs.parallelCore$getChunkSafe(minSectionX + dx, minSectionZ + dz);
            }
        }
    }

    @Nullable
    public ChunkAccess getChunk(int sectionX, int sectionZ) {
        int gx = sectionX - minSectionX;
        int gz = sectionZ - minSectionZ;
        if (gx < 0 || gx >= sizeX || gz < 0 || gz >= sizeZ) return null;
        return chunks[gx][gz];
    }
}
