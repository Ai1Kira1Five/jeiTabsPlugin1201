package com.aikirafive.jeiendlesstabs.client;

import com.aikirafive.jeiendlesstabs.JeiEndlessTabs;
import com.aikirafive.jeiendlesstabs.client.gui.JeiStyleTabButton;
import com.aikirafive.jeiendlesstabs.container.VirtualSlot;
import com.aikirafive.jeiendlesstabs.jei.JeiAddonPlugin;
import com.mojang.logging.LogUtils;
import mezz.jei.api.runtime.IBookmarkOverlay;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.Slot;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.slf4j.Logger;

public class ClientGuiEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ResourceLocation SLOT_BACKGROUND = ResourceLocation.fromNamespaceAndPath(JeiEndlessTabs.MODID, "textures/gui/extra_slots.png");

    private static final int PANEL_WIDTH = 140;
    private static final int PANEL_HEIGHT = 110;

    public static PanelArea getJeiAnchoredArea(AbstractContainerScreen<?> screen) {
        IJeiRuntime runtime = JeiAddonPlugin.getRuntime();
        if (runtime != null) {
            IBookmarkOverlay bookmarkOverlay = runtime.getBookmarkOverlay();
            if (bookmarkOverlay != null) {
                try {
                    java.lang.reflect.Method areaMethod = null;

                    // ИСПРАВЛЕНИЕ: Сканируем методы и ищем тот, который возвращает СТРОГО net.minecraft.client.renderer.Rect2i
                    // Это полностью отсекает левый класс InputArea и предотвращает NoSuchMethodException!
                    for (java.lang.reflect.Method m : bookmarkOverlay.getClass().getMethods()) {
                        if (m.getParameterCount() == 0 && m.getReturnType() == net.minecraft.client.renderer.Rect2i.class) {
                            areaMethod = m;
                            break;
                        }
                    }

                    // Если по строгому классу метод не нашелся из-за обфускации, ищем по простому имени возвращаемого типа Rect2i
                    if (areaMethod == null) {
                        for (java.lang.reflect.Method m : bookmarkOverlay.getClass().getMethods()) {
                            String retName = m.getReturnType().getSimpleName();
                            if (m.getParameterCount() == 0 && (retName.equals("Rect2i") || retName.equals("IRect2i"))) {
                                areaMethod = m;
                                break;
                            }
                        }
                    }

                    if (areaMethod != null) {
                        net.minecraft.client.renderer.Rect2i rect = (net.minecraft.client.renderer.Rect2i) areaMethod.invoke(bookmarkOverlay);
                        if (rect != null) {
                            int jeiX = rect.getX();
                            int jeiY = rect.getY();
                            int jeiWidth = rect.getWidth();
                            int jeiHeight = rect.getHeight();

                            // Центрируем панель строго по середине ширины столбца JEI закладок
                            int x = jeiX + (jeiWidth - PANEL_WIDTH) / 2;
                            int y = jeiY + jeiHeight - PANEL_HEIGHT - 4;

                            LOGGER.info("[JeiAddon Log] SUCCESSFULLY Anchored to JEI -> PanelX: {}, PanelY: {}", x, y);
                            return new PanelArea(x, y);
                        }
                    }
                } catch (Throwable t) {
                    LOGGER.error("[JeiAddon Log] Error executing reflection geometry math", t);
                }
            }
        }
        // Стабильный фоллбэк выравнивания, если JEI скрыт
        return new PanelArea(screen.getGuiLeft() - 147, screen.getGuiTop() + 38);
    }

    private static void updateSlotsPosition(AbstractContainerScreen<?> screen) {
        PanelArea area = getJeiAnchoredArea(screen);

        int startX = area.x() - screen.getGuiLeft() + 7;
        int startY = area.y() - screen.getGuiTop() + 5;
        int localIndex = 0;

        for (Slot slot : screen.getMenu().slots) {
            if (slot instanceof VirtualSlot) {
                int row = localIndex / 8;
                int col = localIndex % 8;

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

            int btnSize = 14;
            int startX = area.x() + 3;
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
            PanelArea area = getJeiAnchoredArea(screen);
            event.getGuiGraphics().blit(SLOT_BACKGROUND, area.x(), area.y(), 0, 0, PANEL_WIDTH, PANEL_HEIGHT, PANEL_WIDTH, PANEL_HEIGHT);
            updateSlotsPosition(screen);
        }
    }

    public record PanelArea(int x, int y) {}
}
