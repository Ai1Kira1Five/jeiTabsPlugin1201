package com.aikirafive.jeiendlesstabs.mixin;

import com.aikirafive.jeiendlesstabs.capability.TabInventoryCapability;
import com.aikirafive.jeiendlesstabs.container.ContainerAddonState;
import com.aikirafive.jeiendlesstabs.container.VirtualSlot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;

public class JeiAddonMasterMixin {

    @Mixin(InventoryMenu.class)
    public static abstract class MenuMixin extends AbstractContainerMenu implements ContainerAddonState {

        protected MenuMixin(net.minecraft.world.inventory.MenuType<?> type, int containerId) {
            super(type, containerId);
        }

        @Unique private int jeiAddon$currentTab = 0;
        @Unique private final List<VirtualSlot> jeiAddon$customSlots = new ArrayList<>();

        @Inject(method = "<init>", at = @At("RETURN"))
        private void onInventoryMenuInit(Inventory playerInventory, boolean isServerSide, Player player, CallbackInfo ci) {
            if (player != null) {
                player.getCapability(TabInventoryCapability.INSTANCE).ifPresent(handler -> {
                    // Сервер чисто инициализирует слоты в памяти в точке (0, 0).
                    // Здесь БОЛЬШЕ НЕТ вызовов клиентских методов, игра физически не сможет крашнуться!
                    int slotIndex = 0;
                    for (int row = 0; row < 6; row++) {
                        for (int col = 0; col < 8; col++) {
                            VirtualSlot virtualSlot = new VirtualSlot(handler, slotIndex++, 0, 0);
                            this.addSlot(virtualSlot);
                            jeiAddon$customSlots.add(virtualSlot);
                        }
                    }
                    jeiAddon$updateSlotIndexes();
                });
            }
        }

        @Override
        public void jeiAddon$setCurrentTab(int tabIndex) {
            this.jeiAddon$currentTab = tabIndex;
            jeiAddon$updateSlotIndexes();
        }

        @Override
        public int jeiAddon$getCurrentTab() {
            return this.jeiAddon$currentTab;
        }

        @Override
        public void jeiAddon$updateSlotIndexes() {
            for (VirtualSlot slot : jeiAddon$customSlots) {
                slot.updateOffset(this.jeiAddon$currentTab);
            }
        }
    }

    @Mixin(AbstractContainerScreen.class)
    public static class ScreenMixin {
        @Inject(method = "render", at = @At("TAIL"))
        private void onRenderTail(GuiGraphics graphics, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
            AbstractContainerScreen<?> screen = (AbstractContainerScreen<?>)(Object)this;
            int guiLeft = screen.getGuiLeft();
            int guiTop = screen.getGuiTop();

            if (screen.getMenu() != null && screen.getMenu().slots != null) {
                for (Slot slot : screen.getMenu().slots) {
                    if (slot instanceof VirtualSlot) {
                        int actualX = guiLeft + slot.x;
                        int actualY = guiTop + slot.y;

                        // Рендерим серые ванильные рамки ячеек поверх нашей подложки
                        graphics.blit(AbstractContainerScreen.INVENTORY_LOCATION, actualX - 1, actualY - 1, 7, 83, 18, 18);

                        // Полупрозрачный красный цвет для контроля положения ячеек
                        //graphics.fill(actualX, actualY, actualX + 16, actualY + 16, 0x80FF0000);
                    }
                }
            }
        }
    }
}
