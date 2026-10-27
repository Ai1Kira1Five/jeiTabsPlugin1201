package com.aikirafive.jeiendlesstabs.capability;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.items.ItemStackHandler;

public class TabInventoryCapability {
    public static final Capability<ItemStackHandler> INSTANCE = CapabilityManager.get(new CapabilityToken<>() {});
}