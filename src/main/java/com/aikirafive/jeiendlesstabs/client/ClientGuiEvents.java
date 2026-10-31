package com.aikirafive.jeiendlesstabs.client;

import com.aikirafive.jeiendlesstabs.JeiEndlessTabs;
import com.aikirafive.jeiendlesstabs.client.gui.JeiStyleTabButton;
import com.aikirafive.jeiendlesstabs.container.VirtualSlot;
import com.aikirafive.jeiendlesstabs.jei.JeiTabManager;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Unique;

public class ClientGuiEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation SLOT_BACKGROUND = ResourceLocation.fromNamespaceAndPath(JeiEndlessTabs.MODID, "textures/gui/extra_slots.png");

    private static final int PANEL_HEIGHT = 110;
    private static final int X_OFFSET = 4;
    private static final int Y_OFFSET_UP_HISTORY = 20;

    @Unique
    private int ticksToSync = -1;

    public static PanelArea getJeiAnchoredArea(AbstractContainerScreen<?> screen) {
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int x = X_OFFSET;
        int y = screenHeight - Y_OFFSET_UP_HISTORY - PANEL_HEIGHT;
        return new PanelArea(x, y);
    }

    private static void updateSlotsPosition(AbstractContainerScreen<?> screen) {
        PanelArea area = getJeiAnchoredArea(screen);

        int startX = area.x() - screen.getGuiLeft() + 7;
        int startY = area.y() - screen.getGuiTop() + 5;
        int localIndex = 0;

        for (Slot slot : screen.getMenu().slots) {
            if (slot instanceof VirtualSlot) {
                int row = localIndex / 9;
                int col = localIndex % 9;

                int targetX = startX + (col * 18);
                int targetY = startY + (row * 18);

                if (slot instanceof com.aikirafive.jeiendlesstabs.container.ISlotCoordinates coordinatesSetter) {
                    coordinatesSetter.jeiAddon$setXCoord(targetX);
                    coordinatesSetter.jeiAddon$setYCoord(targetY);
                }

                localIndex++;
            }
        }
    }

    @SubscribeEvent
    public void onJeiAddonGuiInit(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            PanelArea area = getJeiAnchoredArea(screen);

            int btnSize = 17;
            int startX = area.x() + 7;
            int startY = area.y() - 16;

            for (int i = 0; i < 9; i++) {
                int xOffset = startX + (i * (btnSize + 1));
                event.addListener(new JeiStyleTabButton(xOffset, startY, btnSize, btnSize, i));
            }

            updateSlotsPosition(screen);
        }
    }

    @SubscribeEvent
    public void onGuiRenderPre(ScreenEvent.Render.Pre event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?> screen) {
            updateSlotsPosition(screen);
        }
    }

    public record PanelArea(int x, int y) {}

    // Ловим нажатие клавиши 'A' (Добавление/удаление закладок в JEI)
    @SubscribeEvent
    public void onKeyReleased(ScreenEvent.KeyReleased.Post event) {
        if (event.getScreen() instanceof AbstractContainerScreen<?>) {
            // 65 — это код клавиши 'A'.
            if (event.getKeyCode() == 65) {
                // Если сейчас идет системное обновление вкладок, игнорируем нажатия
                if (JeiTabManager.isUpdatingJei()) return;

                Minecraft mc = Minecraft.getInstance();
                if (mc.screen != null) {
                    // Критически важно: если игрок пишет в поисковую строку JEI или любое другое текстовое поле,
                    // мы полностью блокируем запуск таймера синхронизации, чтобы не стереть текущую вкладку.
                    Object focused = mc.screen.getFocused();
                    if (focused instanceof EditBox) {
                        return;
                    }
                }

                // Запускаем отложенный таймер сохранения (4 тика для стабильности Delayed Executor в JEI)
                ticksToSync = 4;
            }
        }
    }

    // Синхронизатор тиков для безопасного сохранения данных по кнопке 'A'
    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && ticksToSync > 0) {
            ticksToSync--;
            if (ticksToSync == 0) {
                // Вызываем сохранение и синхронизацию только если не активен процесс обновления JEI
                if (!JeiTabManager.isUpdatingJei()) {
                    JeiTabManager.syncAndSaveActiveTab();
                }
            }
        }
    }
}
