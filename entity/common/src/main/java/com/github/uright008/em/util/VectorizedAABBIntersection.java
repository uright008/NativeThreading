package com.github.uright008.em.util;

import net.minecraft.world.phys.AABB;
import java.util.function.BiConsumer;

public final class VectorizedAABBIntersection {

    private static final int UNROLL_FACTOR = 4;

    private VectorizedAABBIntersection() {}

    public static <T> void intersectBatch(
            AABB explosionBounds,
            float[] xminArr, float[] yminArr, float[] zminArr,
            float[] xmaxArr, float[] ymaxArr, float[] zmaxArr,
            AABB[] boxes, T[] objects, int length,
            BiConsumer<AABB, T> consumer) {

        float minX = (float) explosionBounds.minX;
        float minY = (float) explosionBounds.minY;
        float minZ = (float) explosionBounds.minZ;
        float maxX = (float) explosionBounds.maxX;
        float maxY = (float) explosionBounds.maxY;
        float maxZ = (float) explosionBounds.maxZ;

        int i = 0;
        int unrolledEnd = length - (length % UNROLL_FACTOR);

        for (; i < unrolledEnd; i += UNROLL_FACTOR) {
            checkAndAccept(minX, minY, minZ, maxX, maxY, maxZ, xminArr, yminArr, zminArr, xmaxArr, ymaxArr, zmaxArr, boxes, objects, i, consumer);
            checkAndAccept(minX, minY, minZ, maxX, maxY, maxZ, xminArr, yminArr, zminArr, xmaxArr, ymaxArr, zmaxArr, boxes, objects, i + 1, consumer);
            checkAndAccept(minX, minY, minZ, maxX, maxY, maxZ, xminArr, yminArr, zminArr, xmaxArr, ymaxArr, zmaxArr, boxes, objects, i + 2, consumer);
            checkAndAccept(minX, minY, minZ, maxX, maxY, maxZ, xminArr, yminArr, zminArr, xmaxArr, ymaxArr, zmaxArr, boxes, objects, i + 3, consumer);
        }

        for (; i < length; i++) {
            checkAndAccept(minX, minY, minZ, maxX, maxY, maxZ, xminArr, yminArr, zminArr, xmaxArr, ymaxArr, zmaxArr, boxes, objects, i, consumer);
        }
    }

    private static <T> void checkAndAccept(
            float minX, float minY, float minZ,
            float maxX, float maxY, float maxZ,
            float[] xminArr, float[] yminArr, float[] zminArr,
            float[] xmaxArr, float[] ymaxArr, float[] zmaxArr,
            AABB[] boxes, T[] objects, int idx,
            BiConsumer<AABB, T> consumer) {

        if (xminArr[idx] <= maxX && xmaxArr[idx] >= minX &&
            yminArr[idx] <= maxY && ymaxArr[idx] >= minY &&
            zminArr[idx] <= maxZ && zmaxArr[idx] >= minZ &&
            boxes[idx] != null && objects[idx] != null) {
            consumer.accept(boxes[idx], objects[idx]);
        }
    }
}
