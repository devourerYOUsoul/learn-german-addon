package com.deutschimspiel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** A flat button painted with the colours of the current theme. */
public class ThemedButton extends Button {
    private final Theme theme;

    public ThemedButton(int x, int y, int w, int h, Component message, OnPress onPress, Theme theme) {
        super(x, y, w, h, message, onPress, DEFAULT_NARRATION);
        this.theme = theme;
    }

    @Override
    public void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        int c = isHoveredOrFocused() ? theme.btnHover() : theme.btn();
        g.fill(x + 1, y, x + w - 1, y + h, c);
        g.fill(x, y + 1, x + w, y + h - 1, c);
        g.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, 0x55000000);
        if (isHoveredOrFocused()) {
            g.renderOutline(x, y, w, h, theme.accent());
        }
        g.drawCenteredString(Minecraft.getInstance().font, getMessage(), x + w / 2, y + (h - 8) / 2, theme.btnText());
    }
}
