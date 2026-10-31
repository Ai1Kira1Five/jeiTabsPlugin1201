package com.aikirafive.jeiendlesstabs.client.gui;

import com.aikirafive.jeiendlesstabs.container.ContainerAddonState;
import com.aikirafive.jeiendlesstabs.jei.JeiTabManager;
import com.aikirafive.jeiendlesstabs.jei.JeiAddonPlugin; // Подставьте ваш класс плагина JEI
import com.aikirafive.jeiendlesstabs.network.NetworkHandler;
import com.aikirafive.jeiendlesstabs.network.PacketChangeTab;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.helpers.IGuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;

public class JeiStyleTabButton extends AbstractButton {
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
        boolean isActive = JeiTabManager.getCurrentTab() == this.tabIndex;
        boolean isHovered = this.isHoveredOrFocused();

        int x = this.getX();
        int y = this.getY();
        int w = this.width;
        int h = this.height;

        // 1. Рисуем основное серое тело кнопки (цвета подобраны точно под палитру JEI)
        int backgroundColor = isActive ? 0xFF555555 : 0xFF767676;
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, backgroundColor);

        // 2. Рисуем пиксельные границы для создания 3D-эффекта (Bevel), как у оригинальных кнопок JEI
        int thinBorderColor = 0xFF2D2D2D; // Темный контур всей кнопки
        graphics.fill(x, y, x + w, y + 1, thinBorderColor); // Верх
        graphics.fill(x, y + h - 1, x + w, y + h, thinBorderColor); // Низ
        graphics.fill(x, y, x + 1, y + h, thinBorderColor); // Лево
        graphics.fill(x + w - 1, y, x + w, y + h, thinBorderColor); // Право

        if (!isActive) {
            // Для неактивной кнопки делаем светлые внутренние блики (эффект объема)
            graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFFBDBDBD); // Светлый блик сверху
            graphics.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFFBDBDBD); // Светлый блик слева
        } else {
            // Для активной (вдавленной) кнопки делаем внутренние тени сверху и слева
            graphics.fill(x + 1, y + 1, x + w - 1, y + 2, 0xFF3A3A3A);
            graphics.fill(x + 1, y + 1, x + 2, y + h - 1, 0xFF3A3A3A);
        }

        // 3. Дополнительный эффект наведения мыши
        if (isHovered && !isActive) {
            // Полупрозрачная белая засветка при наведении
            graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0x30FFFFFF);
        }

        // 4. Отрисовка цифр (номеров табов)
        Font font = Minecraft.getInstance().font;
        String text = this.getMessage().getString();

        // Золотистый цвет для активного таба, светло-серый для остальных
        int color = isActive ? 0xFFFFA0 : 0xE0E0E0;

        // Смещение текста на 1 пиксель вниз-вправо при нажатии
        int textXOffset = isActive ? 1 : 0;
        int textYOffset = isActive ? 1 : 0;

        graphics.drawString(font, text,
                x + (w - font.width(text)) / 2 + textXOffset,
                y + (h - 8) / 2 + textYOffset,
                color,
                true // Включаем тень шрифта
        );
    }



    @Override
    protected void updateWidgetNarration(NarrationElementOutput output) {
        output.add(NarratedElementType.TITLE, Component.translatable("narration.button", this.getMessage()));
    }
}
