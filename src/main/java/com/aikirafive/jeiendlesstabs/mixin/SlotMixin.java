package com.aikirafive.jeiendlesstabs.mixin;

import com.aikirafive.jeiendlesstabs.container.ISlotCoordinates;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mutable;

@Mixin(Slot.class)
public abstract class SlotMixin implements ISlotCoordinates {

    @Shadow @Final @Mutable public int x;
    @Shadow @Final @Mutable public int y;

    @Override
    public void jeiAddon$setXCoord(int newX) {
        this.x = newX;
    }

    @Override
    public void jeiAddon$setYCoord(int newY) {
        this.y = newY;
    }
}
