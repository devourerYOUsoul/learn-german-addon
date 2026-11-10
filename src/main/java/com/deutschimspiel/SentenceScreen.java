package com.deutschimspiel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.Set;

public class SentenceScreen extends Screen {
    private static final int PW = 320;
    private static final int PH = 206;

    private final Action action;
    private final Subject subject;
    private final GermanWords.Word word;
    private final Config.Tense tense;
    private final boolean food;
    private final Set<String> strict;
    private final Set<String> bare;
    private final String primary;
    private final Theme theme;

    private EditBox input;
    private Component feedback = Component.empty();
    private Component penalty = Component.empty();
    private int feedbackColor;
    private int hintLevel = 0;
    private boolean solved = false;
    private boolean failedOnce = false;
    private boolean recorded = false;

    public SentenceScreen(Action action, Subject subject) {
        super(Component.translatable("deutschimspiel.prompt.title"));
        this.action = action;
        this.subject = subject;
        this.word = subject.word();
        this.tense = Config.TENSE.get();
        this.food = subject.food();
        this.strict = Sentences.strict(action, word, tense, food);
        this.bare = Sentences.bare(action, word, tense, food);
        this.primary = Sentences.primary(action, word, tense, food);
        this.theme = Theme.of(Config.THEME.get());
        this.feedbackColor = theme.text();
    }

    private int px() { return (this.width - PW) / 2; }
    private int py() { return (this.height - PH) / 2; }

    @Override
    protected void init() {
        int x = px();
        int y = py();
        input = new EditBox(this.font, x + 22, y + 116, PW - 44, 10, Component.empty());
        input.setBordered(false);
        input.setTextColor(theme.text());
        input.setMaxLength(120);
        addRenderableWidget(input);
        setInitialFocus(input);

        int bw = 68;
        int gap = 5;
        int by = y + 142;
        int bx = x + 16;
        addRenderableWidget(new ThemedButton(bx, by, bw, 20,
                Component.translatable("deutschimspiel.button.check"), b -> check(), theme));
        addRenderableWidget(new ThemedButton(bx + (bw + gap), by, bw, 20,
                Component.translatable("deutschimspiel.button.hint"), b -> hint(), theme));
        addRenderableWidget(new ThemedButton(bx + 2 * (bw + gap), by, bw, 20,
                Component.translatable("deutschimspiel.button.answer"), b -> showAnswer(), theme));
        addRenderableWidget(new ThemedButton(bx + 3 * (bw + gap), by, bw, 20,
                Component.translatable("deutschimspiel.button.close"), b -> onClose(), theme));
    }

    private Component promptComponent() {
        String which = action == Action.SMELT ? (food ? "smelt_food" : "smelt_other") : action.key;
        String t = tense == Config.Tense.PRESENT ? "present" : "perfect";
        return Component.translatable("deutschimspiel.prompt." + which + "." + t, subject.name());
    }

    private void markFailed() {
        if (!failedOnce) {
            failedOnce = true;
            if (!recorded) {
                recorded = true;
                Progress.record(action, subject.key(), false);
            }
        }
    }

    private void check() {
        if (solved) { onClose(); return; }
        String n = Sentences.norm(input.getValue());
        if (n.isEmpty()) return;
        boolean ok = strict.contains(n) || (word.objG() == 'p' && bare.contains(n));
        boolean almost = !ok && bare.contains(n);
        if (ok || almost) {
            solved = true;
            if (!recorded) {
                recorded = true;
                Progress.record(action, subject.key(), !failedOnce && hintLevel == 0 && ok);
            }
            if (ok) {
                feedback = Component.translatable("deutschimspiel.feedback.correct");
                feedbackColor = theme.ok();
            } else {
                feedback = Component.translatable("deutschimspiel.feedback.almost", primary);
                feedbackColor = theme.warn();
            }
            penalty = Component.empty();
            Punishment.onCorrect();
        } else {
            markFailed();
            feedback = Component.translatable("deutschimspiel.feedback.wrong");
            feedbackColor = theme.bad();
            if (Punishment.onWrong()) {
                List<Penalty> opts = Punishment.available();
                if (!opts.isEmpty()) {
                    Minecraft.getInstance().setScreen(new WheelScreen(opts));
                    return;
                }
            }
            penalty = Punishment.canWheel()
                    ? Component.translatable("deutschimspiel.wheel.warning", Punishment.remaining())
                    : Component.empty();
        }
    }

    private void hint() {
        hintLevel = Math.min(2, hintLevel + 1);
    }

    private void showAnswer() {
        markFailed();
        hintLevel = 2;
        feedback = Component.translatable("deutschimspiel.feedback.answer", primary);
        feedbackColor = theme.warn();
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            check();
            return true;
        }
        return super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    private static void fillRound(GuiGraphics g, int x, int y, int w, int h, int c) {
        g.fill(x + 1, y, x + w - 1, y + h, c);
        g.fill(x, y + 1, x + w, y + h - 1, c);
    }

    private static int lerp(int a, int b, double t) {
        int r = (int) (((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t);
        int gr = (int) (((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = (int) ((a & 255) * (1 - t) + (b & 255) * t);
        return 0xFF000000 | (r << 16) | (gr << 8) | bl;
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.renderBackground(g, mouseX, mouseY, partialTick);
        int x = px();
        int y = py();
        long now = System.currentTimeMillis();

        int borderCol = theme.border();
        if (theme.pulse()) {
            borderCol = lerp(theme.border(), theme.border2(), 0.5 + 0.5 * Math.sin(now / 600.0));
        }
        fillRound(g, x - 3, y - 3, PW + 6, PH + 6, 0x55000000);
        fillRound(g, x - 1, y - 1, PW + 2, PH + 2, borderCol);
        fillRound(g, x + 1, y + 1, PW - 2, PH - 2, theme.bg());

        if (theme.id() == Config.ThemeId.PERGAMENT) {
            g.renderOutline(x + 4, y + 4, PW - 8, PH - 8, theme.border2());
        }
        if (theme.id() == Config.ThemeId.MODERN) {
            g.fill(x + 1, y + 1, x + PW - 1, y + 4, theme.accent());
        }
        if (theme.sparkles()) {
            for (int i = 0; i < 24; i++) {
                int sx = x + 6 + (i * 97) % (PW - 12);
                int sy = y + 6 + (i * 53) % (PH - 12);
                int a = (int) (40 + 120 * (0.5 + 0.5 * Math.sin(now / 450.0 + i * 1.7)));
                int c = (a << 24) | 0xE9D5FF;
                g.fill(sx, sy, sx + 1, sy + 1, c);
                if (i % 5 == 0) {
                    g.fill(sx - 1, sy, sx + 2, sy + 1, c);
                    g.fill(sx, sy - 1, sx + 1, sy + 2, c);
                }
            }
        }
        g.fill(x + 16, y + 44, x + 72, y + 100, theme.slot());
        g.renderOutline(x + 16, y + 44, 56, 56, theme.border2());
        g.fill(x + 16, y + 110, x + PW - 16, y + 132, theme.field());
        g.renderOutline(x + 16, y + 110, PW - 32, 22, theme.fieldBorder());
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = px();
        int y = py();

        Component chip = Component.translatable("deutschimspiel.chip." + action.key);
        int chipW = this.font.width(chip) + 10;
        int chipTop = theme.id() == Config.ThemeId.MODERN ? y + 12 : y + 11;
        g.fill(x + 16, chipTop, x + 16 + chipW, chipTop + 13, theme.accent());
        g.drawString(this.font, chip, x + 21, chipTop + 3, theme.accentText(), false);
        g.drawString(this.font, this.title, x + 16 + chipW + 8, chipTop + 3, theme.title(), false);

        g.pose().pushPose();
        g.pose().translate(x + 20, y + 48, 0);
        g.pose().scale(3.0F, 3.0F, 1.0F);
        g.renderItem(subject.icon(), 0, 0);
        g.pose().popPose();

        List<FormattedCharSequence> lines = this.font.split(promptComponent(), PW - 100);
        int ly = y + 48;
        for (int i = 0; i < Math.min(2, lines.size()); i++) {
            g.drawString(this.font, lines.get(i), x + 84, ly, theme.text(), false);
            ly += 11;
        }
        if (hintLevel >= 1) {
            g.drawString(this.font,
                    Component.translatable("deutschimspiel.hint.noun", Sentences.objectDef(word)),
                    x + 84, y + 76, theme.hint(), false);
        }
        if (hintLevel >= 2) {
            g.drawString(this.font,
                    Component.translatable("deutschimspiel.hint.pattern", Sentences.pattern(action, tense, food)),
                    x + 84, y + 88, theme.hint(), false);
        }

        g.drawCenteredString(this.font, feedback, x + PW / 2, y + 168, feedbackColor);
        g.drawCenteredString(this.font, penalty, x + PW / 2, y + 179, theme.bad());
        Progress.Data d = Progress.get();
        int streak = d.streak.getOrDefault(Progress.key(action, subject.key()), 0);
        g.drawCenteredString(this.font,
                Component.translatable("deutschimspiel.footer", d.correct, d.wrong, streak),
                x + PW / 2, y + 192, theme.hint());
    }
}
