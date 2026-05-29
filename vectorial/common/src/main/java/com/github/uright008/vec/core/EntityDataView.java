package com.github.uright008.vec.core;

/**
 * Provides {@code double[]} views of entity spatial data for
 * SIMD batch processing via {@link SimdBatchOps}.
 *
 * <p>Implementations (e.g. Vectorial's SoAStore) expose their
 * contiguous arrays through this interface, enabling batch
 * distance checks, AABB intersection, and other SIMD operations.</p>
 *
 * <p>An entity's data is at {@code slotForEntity(id)} in each array.
 * Slots are stable for the entity's lifetime; freed slots are zeroed.</p>
 */
public interface EntityDataView {

    /** Entity X positions (slot-indexed, contiguous). */
    double[] posX();

    /** Entity Y positions (slot-indexed, contiguous). */
    double[] posY();

    /** Entity Z positions (slot-indexed, contiguous). */
    double[] posZ();

    /** Entity AABB min X (slot-indexed, contiguous). */
    double[] bbMinX();

    /** Entity AABB min Y (slot-indexed, contiguous). */
    double[] bbMinY();

    /** Entity AABB min Z (slot-indexed, contiguous). */
    double[] bbMinZ();

    /** Entity AABB max X (slot-indexed, contiguous). */
    double[] bbMaxX();

    /** Entity AABB max Y (slot-indexed, contiguous). */
    double[] bbMaxY();

    /** Entity AABB max Z (slot-indexed, contiguous). */
    double[] bbMaxZ();

    /**
     * Returns the slot index for the given entity ID,
     * or -1 if the entity is not registered.
     */
    int slotForEntity(int entityId);

    /** Number of allocated slots (not all may be active). */
    int slotCount();
}
