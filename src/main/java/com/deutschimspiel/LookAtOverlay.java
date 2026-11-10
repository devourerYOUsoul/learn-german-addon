package com.deutschimspiel;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.neoforge.client.event.RenderGuiEvent;

/** Small panel with the German name of the block or dropped item under the crosshair. */
public final class LookAtOverlay {
    private LookAtOverlay() {}

    public static void render(RenderGuiEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        if (!Config.SHOW_LOOKAT.get() || mc.player == null || mc.level == null
                || mc.screen != null || mc.options.hideGui) return;
        HitResult hit = mc.hitResult;
        if (hit == null) return;

        ItemStack stack = ItemStack.EMPTY;
        if (hit instanceof BlockHitResult bhr && hit.getType() == HitResult.Type.BLOCK) {
            BlockState st = mc.level.getBlockState(bhr.getBlockPos());
            Item item = st.getBlock().asItem();
            if (item != Items.AIR) stack = new ItemStack(item);
        } else if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof ItemEntity ie) {
            stack = ie.getItem();
        }
        if (stack.isEmpty()) return;
        GermanWords.Word w = GermanWords.lookup(stack);
        if (w == null) return;

        Component line = w.tooltipLine();
        Theme theme = Theme.of(Config.THEME.get());
        GuiGraphics g = e.getGuiGraphics();
        int pad = 7;
        int bw = mc.font.width(line) + pad * 2;
        int bh = 17;
        int x = (mc.getWindow().getGuiScaledWidth() - bw) / 2 + Config.LOOK_X.get();
        int y = Config.LOOK_Y.get();
        g.fill(x, y, x + bw, y + bh, 0xE0000000 | (theme.bg() & 0xFFFFFF));
        g.renderOutline(x, y, bw, bh, theme.accent());
        g.drawString(mc.font, line, x + pad, y + 5, 0xFFFFFFFF, true);
    }
}
