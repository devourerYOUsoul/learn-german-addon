package com.deutschimspiel;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.Set;

public class SentenceScreen extends Screen {
    private final Action action;
    private final ItemStack stack;
    private final GermanWords.Word word;
    private final Config.Tense tense;
    private final boolean food;
    private final Set<String> strict;
    private final Set<String> bare;
    private final String primary;

    private EditBox input;
    private Component feedback = Component.empty();
    private int feedbackColor = 0xFFFFFFFF;
    private int hintLevel = 0;
    private boolean solved = false;
    private boolean failedOnce = false;
    private boolean recorded = false;

    public SentenceScreen(Action action, ItemStack stack, GermanWords.Word word) {
        super(Component.translatable("deutschimspiel.prompt.title"));
        this.action = action;
        this.stack = stack;
        this.word = word;
        this.tense = Config.TENSE.get();
        this.food = stack.has(DataComponents.FOOD);
        this.strict = Sentences.strict(action, word, tense, food);
        this.bare = Sentences.bare(action, word, tense, food);
        this.primary = Sentences.primary(action, word, tense, food);
    }

    @Override
    protected void init() {
        int cx = this.width / 2;
        int cy = this.height / 2;
        input = new EditBox(this.font, cx - 130, cy, 260, 20, Component.empty());
        input.setMaxLength(120);
        addRenderableWidget(input);
        setInitialFocus(input);

        addRenderableWidget(Button.builder(Component.translatable("deutschimspiel.button.check"), b -> check())
                .bounds(cx - 130, cy + 28, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("deutschimspiel.button.hint"), b -> hint())
                .bounds(cx - 44, cy + 28, 80, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("deutschimspiel.button.answer"), b -> showAnswer())
                .bounds(cx + 42, cy + 28, 88, 20).build());
        addRenderableWidget(Button.builder(Component.translatable("deutschimspiel.button.close"), b -> onClose())
                .bounds(cx - 40, cy + 54, 80, 20).build());
    }

    private Component promptComponent() {
        String which = action == Action.SMELT ? (food ? "smelt_food" : "smelt_other") : action.key;
        String t = tense == Config.Tense.PRESENT ? "present" : "perfect";
        return Component.translatable("deutschimspiel.prompt." + which + "." + t, stack.getHoverName());
    }

    private void markFailed() {
        if (!failedOnce) {
            failedOnce = true;
            if (!recorded) {
                recorded = true;
                Progress.record(action, stack, false);
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
                Progress.record(action, stack, !failedOnce && hintLevel == 0 && ok);
            }
            if (ok) {
                feedback = Component.translatable("deutschimspiel.feedback.correct");
                feedbackColor = 0xFF55FF55;
            } else {
                feedback = Component.translatable("deutschimspiel.feedback.almost", primary);
                feedbackColor = 0xFFFFFF55;
            }
        } else {
            markFailed();
            feedback = Component.translatable("deutschimspiel.feedback.wrong");
            feedbackColor = 0xFFFF5555;
        }
    }

    private void hint() {
        hintLevel = Math.min(2, hintLevel + 1);
    }

    private void showAnswer() {
        markFailed();
        hintLevel = 2;
        feedback = Component.translatable("deutschimspiel.feedback.answer", primary);
        feedbackColor = 0xFFFFFF55;
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
        return false;
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int cx = this.width / 2;
        int cy = this.height / 2;
        g.drawCenteredString(this.font, this.title, cx, cy - 80, 0xFFFFFF55);
        g.renderItem(stack, cx - 8, cy - 66);
        g.drawCenteredString(this.font, promptComponent(), cx, cy - 42, 0xFFFFFFFF);
        if (hintLevel >= 1) {
            g.drawCenteredString(this.font,
                    Component.translatable("deutschimspiel.hint.noun", Sentences.objectDef(word)),
                    cx, cy - 28, 0xFFAAAAAA);
        }
        if (hintLevel >= 2) {
            g.drawCenteredString(this.font,
                    Component.translatable("deutschimspiel.hint.pattern", Sentences.pattern(action, tense, food)),
                    cx, cy - 16, 0xFFAAAAAA);
        }
        g.drawCenteredString(this.font, feedback, cx, cy + 82, feedbackColor);
    }
}
