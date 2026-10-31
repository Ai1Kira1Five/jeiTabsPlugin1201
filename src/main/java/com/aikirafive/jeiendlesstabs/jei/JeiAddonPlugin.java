package com.aikirafive.jeiendlesstabs.jei;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.registration.IGuiHandlerRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.ResourceLocation;
import java.util.List;

@JeiPlugin
public class JeiAddonPlugin implements IModPlugin {
    private static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath("jei_endless_tabs", "jei_plugin");
    private static IJeiRuntime runtime;
    private static IGuiHelper guiHelper;

    @Override
    public ResourceLocation getPluginUid() {
        return ID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        // Извлекаем IGuiHelper во время ранней стадии инициализации рецептов JEI
        guiHelper = registration.getJeiHelpers().getGuiHelper();
    }

    @Override
    public void onRuntimeAvailable(IJeiRuntime jeiRuntime) {
        runtime = jeiRuntime;

        JeiTabManager.loadTabsFromDisk();
    }

    @Override
    @SuppressWarnings("unchecked")
    public void registerGuiHandlers(IGuiHandlerRegistration registration) {
        Class<AbstractContainerScreen<?>> screenClass = (Class<AbstractContainerScreen<?>>) (Class<?>) AbstractContainerScreen.class;

        registration.addGuiContainerHandler(screenClass, new mezz.jei.api.gui.handlers.IGuiContainerHandler<AbstractContainerScreen<?>>() {
            @Override
            public List<Rect2i> getGuiExtraAreas(AbstractContainerScreen<?> screen) {
                // Запрашиваем динамические координаты нашей панели, увязанной с JEI
                com.aikirafive.jeiendlesstabs.client.ClientGuiEvents.PanelArea area =
                        com.aikirafive.jeiendlesstabs.client.ClientGuiEvents.getJeiAnchoredArea(screen);

                int x = area.x();
                int y = area.y() - 20; // Включаем область кнопок вкладок
                int width = 142;
                int height = 130;

                return List.of(new Rect2i(x, y, width, height));
            }
        });
    }

    public static IJeiRuntime getRuntime() {
        return runtime;
    }

    /**
     * Возвращает графический хелпер JEI для прорисовки оригинальных кнопок и фонов.
     */
    public static IGuiHelper getGuiHelper() {
        return guiHelper;
    }
}
