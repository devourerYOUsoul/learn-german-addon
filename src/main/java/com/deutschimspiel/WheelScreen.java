package com.deutschimspiel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import java.util.List;
import java.util.Random;

/** Wheel of fortune: grows in, spins with easing, the winning sector is pulled out. All punishments are equally likely. */
public class WheelScreen extends Screen {
    private static final long ENTER_MS = 750L;
    private static final long SPIN_MS = 5500L;
    private static final long HOLD_MS = 2800L;
    private static final long BURST_MS = 1500L;
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

    private static double easeBack(double q) {
        double c1 = 1.6;
        double c3 = c1 + 1.0;
        return 1.0 + c3 * Math.pow(q - 1.0, 3) + c1 * Math.pow(q - 1.0, 2);
    }

    private static void disc(GuiGraphics g, int cx, int cy, int r, int color) {
        for (int dy = -r; dy < r; dy++) {
            double oy = dy + 0.5;
            double hw = Math.sqrt(Math.max(0.0, (double) r * r - oy * oy));
            g.fill(cx - (int) Math.ceil(hw), cy + dy, cx + (int) Math.ceil(hw), cy + dy + 1, color);
        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        long now = System.currentTimeMillis();
        long el = now - start;
        double p = Math.min(1.0, el / (double) SPIN_MS);
        double theta = finalAngle * (1.0 - Math.pow(1.0 - p, 4));
        double zt = Math.min(1.0, el / (double) ENTER_MS);
        double z = zt >= 1.0 ? 1.0 : Math.max(0.03, easeBack(zt));
        double fade = Math.min(1.0, el / 350.0);

        int n = options.size();
        double seg = 360.0 / n;
        int cx = this.width / 2;
        int cy = this.height / 2 + 6;
        int base = Math.min(72, Math.max(40, this.height / 2 - 46));
        int radius = Math.max(5, (int) Math.round(base * z));

        if (p >= 1.0 && !applied) {
            applied = true;
            doneAt = now;
            Punishment.apply(options.get(winner));
        }
        long since = applied ? now - doneAt : 0L;

        // light beams behind the wheel
        if (z > 0.3) {
            double rot = el / 80.0;
            for (int k = 0; k < 12; k++) {
                double a = Math.toRadians(k * 30 + rot);
                for (int d = radius + 16; d < radius + 170; d += 3) {
                    int bx = (int) Math.round(cx + Math.sin(a) * d);
                    int by = (int) Math.round(cy - Math.cos(a) * d);
                    int w = 2 + (d - radius) / 14;
                    g.fill(bx - w / 2, by - w / 2, bx + w / 2 + 1, by + w / 2 + 1, 0x0EA070FF);
                }
            }
        }
        // glow
        int glowA = (int) ((0x26 + 0x20 * (0.5 + 0.5 * Math.sin(el / 300.0))) * Math.min(1.0, z));
        disc(g, cx, cy, radius + (int) (14 * z), (glowA << 24) | 0x7B4DFF);

        // title
        int ta = Math.max(8, (int) (255 * fade));
        g.drawCenteredString(this.font, this.title, cx, cy - base - 32, (ta << 24) | (theme.title() & 0xFFFFFF));

        // rune ring turning the other way
        double ringRot = -el / 28.0;
        int ringR = radius + (int) (12 * z);
        for (int k = 0; k < 60; k++) {
            double a = Math.toRadians(k * 6 + ringRot);
            int len = k % 5 == 0 ? 6 : 3;
            for (int s = 0; s < len; s++) {
                int tx = (int) Math.round(cx + Math.sin(a) * (ringR + s));
                int ty = (int) Math.round(cy - Math.cos(a) * (ringR + s));
                g.fill(tx, ty, tx + 1, ty + 1, 0xCCC8A0FF);
            }
        }

        // wheel body: 1px rows, runs merged
        int rim = theme.border2();
        int hub = theme.bg();
        double r2 = (double) radius * radius;
        for (int dy = -radius; dy < radius; dy++) {
            double oy = dy + 0.5;
            double hw = Math.sqrt(Math.max(0.0, r2 - oy * oy));
            int x0 = (int) Math.floor(-hw);
            int x1 = (int) Math.ceil(hw);
            boolean has = false;
            int runStart = x0;
            int runColor = 0;
            for (int dx = x0; dx < x1; dx++) {
                double ox = dx + 0.5;
                double dist = Math.sqrt(ox * ox + oy * oy);
                int color;
                if (dist > radius - 3.5) {
                    color = rim;
                } else if (dist < radius * 0.14) {
                    color = dist > radius * 0.14 - 2.5 ? rim : hub;
                } else {
                    double a2 = Math.toDegrees(Math.atan2(ox, -oy));
                    double local = (((a2 - theta) % 360.0) + 360.0) % 360.0;
                    int idx = Math.min(n - 1, (int) (local / seg));
                    double delta = Math.min(local - idx * seg, (idx + 1) * seg - local);
                    if (dist * Math.toRadians(delta) < 1.0) {
                        color = 0xFFFFFFFF;
                    } else {
                        color = options.get(idx).color;
                        if (applied) color = mul(color, 0.4);
                    }
                }
                if (!has) {
                    has = true;
                    runStart = dx;
                    runColor = color;
                } else if (color != runColor) {
                    g.fill(cx + runStart, cy + dy, cx + dx, cy + dy + 1, runColor);
                    runStart = dx;
                    runColor = color;
                }
            }
            if (has) g.fill(cx + runStart, cy + dy, cx + x1, cy + dy + 1, runColor);
        }

        // the winning sector slides out
        if (applied) {
            double e = Math.min(1.0, since / 420.0);
            double pull = 10.0 * (1.0 - Math.pow(1.0 - e, 3)) * Math.max(0.5, radius / 72.0);
            double midA = Math.toRadians(winner * seg + seg / 2.0 + theta);
            int sx = cx + (int) Math.round(Math.sin(midA) * pull);
            int sy = cy - (int) Math.round(Math.cos(midA) * pull);
            int wr = radius + 3;
            int wcol = mul(options.get(winner).color, 1.1 + 0.15 * Math.sin(since / 110.0));
            double wr2 = (double) wr * wr;
            for (int dy = -wr; dy < wr; dy++) {
                double oy = dy + 0.5;
                double hw = Math.sqrt(Math.max(0.0, wr2 - oy * oy));
                int x0 = (int) Math.floor(-hw);
                int x1 = (int) Math.ceil(hw);
                boolean has = false;
                int runStart = 0;
                int runColor = 0;
                for (int dx = x0; dx <= x1; dx++) {
                    int color = 0;
                    boolean in = false;
                    if (dx < x1) {
                        double ox = dx + 0.5;
                        double dist = Math.sqrt(ox * ox + oy * oy);
                        if (dist > radius * 0.14) {
                            double a2 = Math.toDegrees(Math.atan2(ox, -oy));
                            double local = (((a2 - theta) % 360.0) + 360.0) % 360.0;
                            int idx = Math.min(n - 1, (int) (local / seg));
                            if (idx == winner) {
                                in = true;
                                double delta = Math.min(local - idx * seg, (idx + 1) * seg - local);
                                if (dist * Math.toRadians(delta) < 1.0 || dist > wr - 2.5) {
                                    color = 0xFFFFFFFF;
                                } else {
                                    color = wcol;
                                }
                            }
                        }
                    }
                    if (in) {
                        if (!has) {
                            has = true;
                            runStart = dx;
                            runColor = color;
                        } else if (color != runColor) {
                            g.fill(sx + runStart, sy + dy, sx + dx, sy + dy + 1, runColor);
                            runStart = dx;
                            runColor = color;
                        }
                    } else if (has) {
                        g.fill(sx + runStart, sy + dy, sx + dx, sy + dy + 1, runColor);
                        has = false;
                    }
                }
            }
        }

        // icons
        if (z > 0.25) {
            double iconR = radius * 0.66;
            for (int i = 0; i < n; i++) {
                double a = Math.toRadians(i * seg + seg / 2.0 + theta);
                double ix = cx + Math.sin(a) * iconR;
                double iy = cy - Math.cos(a) * iconR;
                if (applied && i == winner) {
                    double e = Math.min(1.0, since / 420.0);
                    double pull = 10.0 * (1.0 - Math.pow(1.0 - e, 3)) * Math.max(0.5, radius / 72.0);
                    ix += Math.sin(a) * pull;
                    iy -= Math.cos(a) * pull;
                }
                g.pose().pushPose();
                g.pose().translate((float) ix, (float) iy, 0.0F);
                g.pose().scale((float) Math.min(1.0, z), (float) Math.min(1.0, z), 1.0F);
                g.renderItem(new ItemStack(options.get(i).icon.get()), -8, -8);
                g.pose().popPose();
            }
        }

        // pointer
        int ps = Math.max(2, (int) Math.round(10 * Math.min(1.0, z)));
        for (int r = 0; r < ps; r++) {
            int w = ps - r;
            g.fill(cx - w, cy - radius - ps - 4 + r, cx + w + 1, cy - radius - ps - 3 + r, 0xFFE9D5FF);
        }

        // sparkles near the pointer while spinning fast
        if (!applied && z >= 0.99) {
            double speed = finalAngle * 4.0 * Math.pow(1.0 - p, 3) / SPIN_MS * 16.7;
            Random rnd = new Random(now / 45L);
            int count = Math.min(10, (int) (speed / 1.5));
            for (int k = 0; k < count; k++) {
                int x = cx + rnd.nextInt(69) - 34;
                int y = cy - radius - 6 + rnd.nextInt(34) - 8;
                int a = 120 + rnd.nextInt(110);
                g.fill(x, y, x + 2, y + 2, (a << 24) | 0xE9D5FF);
            }
        }

        // result
        if (!applied) {
            if (el > ENTER_MS) {
                int sa = Math.max(8, (int) (255 * Math.min(1.0, (el - ENTER_MS) / 300.0)));
                g.drawCenteredString(this.font, Component.translatable("deutschimspiel.wheel.spinning"),
                        cx, cy + base + 14, (sa << 24) | (theme.hint() & 0xFFFFFF));
            }
        } else {
            double e = Math.min(1.0, since / (double) BURST_MS);
            double ease = 1.0 - Math.pow(1.0 - e, 3);
            double scale = Math.max(0.5, base / 72.0);
            for (int k = 0; k < 52; k++) {
                Random pr = new Random(k * 977L + 13L);
                double a = pr.nextDouble() * Math.PI * 2;
                double sp = (60 + pr.nextDouble() * 180) * scale;
                int a8 = (int) (255 * Math.pow(1.0 - ease, 1.2));
                if (a8 < 8) continue;
                int x = cx + (int) Math.round(Math.sin(a) * sp * ease);
                int y = cy - (int) Math.round(Math.cos(a) * sp * ease);
                int sz = 1 + pr.nextInt(2);
                g.fill(x, y, x + sz + 1, y + sz + 1, (a8 << 24) | 0xFFE678);
            }
            Component name = Component.translatable("deutschimspiel.penalty." + options.get(winner).key + ".name");
            Component line = Component.translatable("deutschimspiel.wheel.result", name);
            int tw = this.font.width(line);
            int ty = cy + base + 12;
            int bw = (int) (tw * 1.5) + 28;
            g.fill(cx - bw / 2, ty - 6, cx + bw / 2, ty + 20, 0xE60F0A1E);
            g.renderOutline(cx - bw / 2, ty - 6, bw, 26, 0xFF000000 | options.get(winner).color);
            g.pose().pushPose();
            g.pose().translate(cx, ty, 0.0F);
            g.pose().scale(1.5F, 1.5F, 1.0F);
            g.drawCenteredString(this.font, line, 0, 0, 0xFF000000 | options.get(winner).color);
            g.pose().popPose();
            if (!closing && since > HOLD_MS) {
                closing = true;
                Minecraft.getInstance().execute(this::onClose);
            }
        }
    }
}
