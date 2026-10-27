package com.aikirafive.jeiendlesstabs;

import com.aikirafive.jeiendlesstabs.capability.TabInventoryCapability;
import com.aikirafive.jeiendlesstabs.capability.TabInventoryProvider;
import com.aikirafive.jeiendlesstabs.network.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.items.ItemStackHandler;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(JeiEndlessTabs.MODID)
public class JeiEndlessTabs {
    public static final String MODID = "jei_endless_tabs";

    public JeiEndlessTabs() {
        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        IEventBus forgeBus = MinecraftForge.EVENT_BUS;

        modBus.addListener(this::setup);
        modBus.addListener(this::registerCapabilities);

        forgeBus.addGenericListener(Entity.class, this::attachCapabilities);

        // КРИТИЧЕСКИЙ ФИНАЛЬНЫЙ ХУК: Регистрируем инстанс очищенного класса событий!
        // Это навсегда уберёт варнинг "never used" и заставит кнопки 1-9 появиться на экране.
        forgeBus.register(new com.aikirafive.jeiendlesstabs.client.ClientGuiEvents());
    }

    private void setup(final FMLCommonSetupEvent event) {
        event.enqueueWork(NetworkHandler::register);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.register(ItemStackHandler.class);
    }

    private void attachCapabilities(AttachCapabilitiesEvent<Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ResourceLocation.fromNamespaceAndPath(MODID, "tabs_inventory"), new TabInventoryProvider());
        }
    }
}