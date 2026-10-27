package com.aikirafive.jeiendlesstabs.container;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.SlotItemHandler;

public class VirtualSlot extends SlotItemHandler {
    private final int baseIndex;
    private int currentOffset = 0;

    public VirtualSlot(IItemHandler itemHandler, int baseIndex, int xPosition, int yPosition) {
        super(itemHandler, baseIndex, xPosition, yPosition);
        this.baseIndex = baseIndex;
    }

    public void updateOffset(int tabIndex) {
        this.currentOffset = tabIndex * 48;
    }

    @Override
    public int getSlotIndex() {
        return this.baseIndex + this.currentOffset;
    }

    // Маркер отладки
    public boolean isDebugSlots() {
        return true;
    }
}