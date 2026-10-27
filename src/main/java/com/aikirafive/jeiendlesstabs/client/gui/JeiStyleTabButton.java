package com.aikirafive.jeiendlesstabs.client.gui;

import com.aikirafive.jeiendlesstabs.container.ContainerAddonState;
import com.aikirafive.jeiendlesstabs.jei.JeiTabManager;
import com.aikirafive.jeiendlesstabs.network.NetworkHandler;
import com.aikirafive.jeiendlesstabs.network.PacketChangeTab;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

public class JeiStyleTabButton extends AbstractButton {
    // Используем текстуру атласа GUI JEI, которая гарантированно зарегистрирована в игре (см. лог)
    private static final ResourceLocation JEI_GUI_ATLAS = ResourceLocation.fromNamespaceAndPath("jei", "textures/atlas/gui.png");
    private final int tabIndex;

    public JeiStyleTabButton(int x, int y, int width, int height, int tabIndex) {
        super(x, y, width, height, Component.literal(String.valueOf(tabIndex + 1)));
        this.tabIndex = tabIndex;
    }

    @Override
    public void onPress() {
        JeiTabManager.switchToTab(this.tabIndex);
        NetworkHandler.CHANNEL.sendToServer(new PacketChangeTab(this.tabIndex));
        if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.containerMenu instanceof ContainerAddonState state) {
            state.jeiAddon$setCurrentTab(this.tabIndex);
        }
    }

    @Override
    protected void renderWidget(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Стандартная иконка квадратной кнопки JEI в их атласе gui.png находится по следующим координатам:
        int u = 0;
        int v = 0;

        boolean isActive = JeiTabManager.getCurrentTab() == this.tabIndex;
        if (this.isHoveredOrFocused() || isActive) {
            u += this.width; // Смещение для подсвеченного состояния кнопки
        }

        // Рендерим из официального атласа JEI
        graphics.blit(JEI_GUI_ATLAS, this.getX(), this.getY(), u, v, this.width, this.height, 256, 256);

        Font font = Minecraft.getInstance().font;
        String text = this.getMessage().getString();
        int color = isActive ? 0xFFFFA0 : 0xE0E0E0;
        graphics.drawString(font, text, this.getX() + (this.width - font.width(text)) / 2, this.getY() + (this.height - 8) / 2, color, true);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("narration.button", this.getMessage()));
    }
}
