package com.deutschimspiel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Random;

/** Wheel of fortune: all enabled punishments have the same chance, one of them lands under the pointer. */
public class WheelScreen extends Screen {
    private static final long SPIN_MS = 5500L;
    private static final long HOLD_MS = 2600L;
    private static final Random RNG = new Random();

    private final List<Penalty> options;
    private final int winner;
    private final double finalAngle;
    private final long start = System.currentTimeMillis();
    private final Theme theme = Theme.of(Config.THEME.get());
    private boolean applied = false;
    private boolean closing = false;
    private long doneAt = 0L;

    public WheelScreen(List<Penalty> options) {
        super(Component.translatable("deutschimspiel.wheel.title"));
        this.options = options;
        int n = options.size();
        this.winner = RNG.nextInt(n);
        double seg = 360.0 / n;
        double local = winner * seg + seg / 2.0 + (RNG.nextDouble() - 0.5) * seg * 0.7;
        this.finalAngle = 360.0 * 5 + (360.0 - local);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return applied;
    }

    private static int mul(int argb, double f) {
        int r = (int) Math.min(255, ((argb >> 16) & 255) * f);
        int g = (int) Math.min(255, ((argb >> 8) & 255) * f);
        int b = (int) Math.min(255, (argb & 255) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        long now = System.currentTimeMillis();
        double p = Math.min(1.0, (now - start) / (double) SPIN_MS);
        double eased = 1.0 - Math.pow(1.0 - p, 3);
        double theta = finalAngle * eased;
        int n = options.size();
        double seg = 360.0 / n;
        int cx = this.width / 2;
        int cy = this.height / 2 + 6;
        int radius = Math.min(72, Math.max(40, this.height / 2 - 46));

        g.drawCenteredString(this.font, this.title, cx, cy - radius - 32, theme.title());

        int rim = theme.border2();
        int hub = theme.bg();
        for (int dy = -radius; dy < radius; dy += 2) {
            for (int dx = -radius; dx < radius; dx += 2) {
                double ox = dx + 1.0;
                double oy = dy + 1.0;
                double d2 = ox * ox + oy * oy;
                if (d2 > (double) radius * radius) continue;
                double dist = Math.sqrt(d2);
                int color;
                if (dist > radius - 3) {
                    color = rim;
                } else if (dist < 9) {
                    color = hub;
                } else {
                    double a = Math.toDegrees(Math.atan2(ox, -oy));
                    double local = (((a - theta) % 360.0) + 360.0) % 360.0;
                    int idx = Math.min(n - 1, (int) (local / seg));
                    double frac = (local - idx * seg) / seg;
                    color = options.get(idx).color;
                    if (frac < 0.015 || frac > 0.985) {
                        color = mul(color, 0.45);
                    } else if (idx % 2 == 1) {
                        color = mul(color, 0.86);
                    }
                }
                g.fill(cx + dx, cy + dy, cx + dx + 2, cy + dy + 2, color);
            }
        }

        double iconR = radius * 0.66;
        for (int i = 0; i < n; i++) {
            double a = Math.toRadians(i * seg + seg / 2.0 + theta);
            int ix = (int) Math.round(cx + Math.sin(a) * iconR) - 8;
            int iy = (int) Math.round(cy - Math.cos(a) * iconR) - 8;
            g.renderItem(new ItemStack(options.get(i).icon.get()), ix, iy);
        }

        // pointer
        for (int r = 0; r < 10; r++) {
            int w = 10 - r;
            g.fill(cx - w, cy - radius - 14 + r, cx + w + 1, cy - radius - 13 + r, 0xFFFFFFFF);
        }

        if (p >= 1.0 && !applied) {
            applied = true;
            doneAt = now;
            Punishment.apply(options.get(winner));
        }

        int ty = cy + radius + 14;
        if (!applied) {
            g.drawCenteredString(this.font, Component.translatable("deutschimspiel.wheel.spinning"), cx, ty, theme.hint());
        } else {
            Component name = Component.translatable("deutschimspiel.penalty." + options.get(winner).key + ".name");
            g.pose().pushPose();
            g.pose().translate(cx, ty, 0);
            g.pose().scale(1.5F, 1.5F, 1.0F);
            g.drawCenteredString(this.font, Component.translatable("deutschimspiel.wheel.result", name), 0, 0, theme.bad());
            g.pose().popPose();
            if (!closing && now - doneAt > HOLD_MS) {
                closing = true;
                Minecraft.getInstance().execute(this::onClose);
            }
        }
    }
}
