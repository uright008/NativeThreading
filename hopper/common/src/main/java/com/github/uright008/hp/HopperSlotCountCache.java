package com.github.uright008.hp;

import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

public final class HopperSlotCountCache {

    private int modCount = 0;
    private int cachedOccupiedSlots = -1;
    private int cachedFullSlots = -1;

    public void markDirty() {
        modCount++;
    }

    public int getOccupiedSlots(Container container) {
        if (cachedOccupiedSlots < 0) {
            recalculate(container);
        }
        return cachedOccupiedSlots;
    }

    public int getFullSlots(Container container) {
        if (cachedFullSlots < 0) {
            recalculate(container);
        }
        return cachedFullSlots;
    }

    private void recalculate(Container container) {
        int occupied = 0;
        int full = 0;
        int size = container.getContainerSize();

        for (int i = 0; i < size; i++) {
            ItemStack stack = container.getItem(i);
            if (!stack.isEmpty()) {
                occupied++;
                if (stack.getCount() >= stack.getMaxStackSize()) {
                    full++;
                }
            }
        }

        cachedOccupiedSlots = occupied;
        cachedFullSlots = full;
    }

    public void invalidate() {
        cachedOccupiedSlots = -1;
        cachedFullSlots = -1;
    }
}
