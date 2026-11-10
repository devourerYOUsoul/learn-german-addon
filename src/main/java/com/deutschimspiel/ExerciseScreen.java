package com.deutschimspiel;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** A2/B1 exercises: typing, word tiles, gaps, one-word gap, find-the-mistake, pick-the-right-one, and chains of them. */
public class ExerciseScreen extends Screen {
    private static final int PW = 340;

    private final Action action;
    private final Subject subject;
    private final List<Exercises.Stage> stages;
    private final Theme theme;
    private final Random rnd = new Random();

    private int cur = 0;
    private int ph = 240;

    // per-stage state
    private Exercises.Mode mode;
    private Exercises.Task task;
    private List<String> disp = new ArrayList<>();     // displayed tokens of the answer
    private boolean[] slotBlank = new boolean[0];
    private String[] slotFill = new String[0];
    private int[] slotBank = new int[0];
    private boolean[] slotWrong = new boolean[0];
    private List<String> bank = new ArrayList<>();
    private boolean[] bankUsed = new boolean[0];
    private String shown = "";
    private String gapAnswer = "";
    private List<String> options = new ArrayList<>();
    private int correctOpt = -1;
    private int picked = -1;
    private final List<Integer> wrongPicks = new ArrayList<>();
    private boolean stageDone = false;
    private int hintLevel = 0;
    private boolean revealed = false;

    // whole-exercise state
    private boolean failedOnce = false;
    private boolean anyHint = false;
    private boolean pendingReward = false;
    private boolean shielded = false;

    // geometry (relative to the panel)
    private List<FormattedCharSequence> promptLines = List.of();
    private final List<FormattedCharSequence> ruLines = new ArrayList<>();
    private List<FormattedCharSequence> shownLines = List.of();
    private int areaTop, areaBottom, fieldY, bankTop, buttonsY, hintY, feedbackY, penaltyY, footerY;
    private int[][] slotRect = new int[0][];
    private int[][] bankRect = new int[0][];
    private int[][] optRect = new int[0][];
    private List<List<FormattedCharSequence>> optLines = new ArrayList<>();
    private int flowBottom;

    private EditBox input;
    private ThemedButton checkBtn;
    private Component feedback = Component.empty();
    private Component penalty = Component.empty();
    private int feedbackColor;

    public ExerciseScreen(Action action, Subject subject, List<Exercises.Stage> stages) {
        super(Component.translatable("deutschimspiel.prompt.title"));
        this.action = action;
        this.subject = subject;
        this.stages = stages;
        this.theme = Theme.of(Config.THEME.get());
        this.feedbackColor = theme.text();
        prepareStage(0);
    }

    private int px() { return (this.width - PW) / 2; }
    private int py() { return (this.height - ph) / 2; }

    // ------------------------------------------------------------ stage setup

    private static String stripDot(String s) {
        return s.endsWith(".") ? s.substring(0, s.length() - 1) : s;
    }

    private void prepareStage(int i) {
        Exercises.Stage st = stages.get(i);
        mode = st.mode();
        task = st.task();
        stageDone = false;
        hintLevel = 0;
        revealed = false;
        picked = -1;
        wrongPicks.clear();
        feedback = Component.empty();
        feedbackColor = theme.text();
        penalty = Component.empty();
        disp = new ArrayList<>();
        List<String> tk = task.tokens();
        for (int j = 0; j < tk.size(); j++) {
            disp.add(j == tk.size() - 1 ? stripDot(tk.get(j)) : tk.get(j));
        }
        int n = disp.size();
        slotBlank = new boolean[n];
        slotFill = new String[n];
        slotBank = new int[n];
        slotWrong = new boolean[n];
        java.util.Arrays.fill(slotBank, -1);
        bank = new ArrayList<>();
        shown = "";
        options = new ArrayList<>();
        correctOpt = -1;
        switch (mode) {
            case TILES -> {
                java.util.Arrays.fill(slotBlank, true);
                bank.addAll(disp);
                bank.addAll(Exercises.distractors(task, disp, 2 + rnd.nextInt(3), rnd));
            }
            case GAP -> {
                int k = 1 + rnd.nextInt(Math.max(1, Math.min(3, n - 1)));
                List<Integer> blanks = Exercises.pickBlanks(task, k, rnd);
                List<String> answers = new ArrayList<>();
                for (int b : blanks) {
                    slotBlank[b] = true;
                    answers.add(disp.get(b));
                }
                bank.addAll(answers);
                bank.addAll(Exercises.distractors(task, answers, 2 + rnd.nextInt(2), rnd));
            }
            case GAPTYPE -> {
                List<Integer> blanks = Exercises.pickBlanks(task, 1, rnd);
                int b = blanks.isEmpty() ? n - 1 : blanks.get(0);
                gapAnswer = disp.get(b);
                StringBuilder sb = new StringBuilder();
                for (int j = 0; j < n; j++) {
                    if (j > 0) sb.append(' ');
                    sb.append(j == b ? "_____" + (gapAnswer.endsWith(",") ? "," : "") : disp.get(j));
                }
                shown = sb.toString() + ".";
            }
            case FIX -> shown = Exercises.corrupt(task, rnd);
            case PICK -> {
                List<String> opts = new ArrayList<>();
                opts.add(task.primary());
                for (int t = 0; t < 8 && opts.size() < 3; t++) {
                    String c = Exercises.corrupt(task, rnd);
                    boolean dup = false;
                    for (String o : opts) if (Sentences.norm(o).equals(Sentences.norm(c))) dup = true;
                    if (!dup) opts.add(c);
                }
                Collections.shuffle(opts, rnd);
                options = opts;
                for (int j = 0; j < opts.size(); j++) {
                    if (Sentences.norm(opts.get(j)).equals(Sentences.norm(task.primary()))) correctOpt = j;
                }
            }
            default -> { }
        }
        bankUsed = new boolean[bank.size()];
        if (mode == Exercises.Mode.TILES || mode == Exercises.Mode.GAP) {
            // shuffle the bank
            List<String> sh = new ArrayList<>(bank);
            Collections.shuffle(sh, rnd);
            bank = sh;
        }
    }

    private Component promptComponent() {
        String which = action == Action.SMELT ? (subject.food() ? "smelt_food" : "smelt_other") : action.key;
        String t = Config.TENSE.get() == Config.Tense.PRESENT ? "present" : "perfect";
        return Component.translatable("deutschimspiel.prompt." + which + "." + t, subject.name());
    }

    // ------------------------------------------------------------ geometry / widgets

    private int[][] flow(List<String> texts, boolean[] fixedBlank, int minW, int y0) {
        int[][] out = new int[texts.size()][];
        int x0 = 20;
        int maxX = PW - 20;
        int cx = x0;
        int cy = y0;
        for (int i = 0; i < texts.size(); i++) {
            int w = Math.max(minW, this.font.width(texts.get(i)) + 8);
            if (cx + w > maxX && cx > x0) {
                cx = x0;
                cy += 17;
            }
            out[i] = new int[]{cx, cy, w, 14};
            cx += w + 4;
        }
        flowBottom = cy + 14;
        return out;
    }

    private void computeGeo() {
        int textW = PW - 92;
        promptLines = this.font.split(promptComponent(), textW);
        ruLines.clear();
        for (String s : task.ru()) ruLines.addAll(this.font.split(Component.literal(s), textW));
        int lines = Math.min(2, promptLines.size()) + Math.min(5, ruLines.size());
        int textBottom = 32 + lines * 11;
        areaTop = Math.max(88, textBottom + 6);
        int bottom;
        switch (mode) {
            case TILES, GAP -> {
                slotRect = flow(disp, null, 22, areaTop + 4);
                int slotsBottom = flowBottom + 4;
                areaBottom = slotsBottom;
                bankTop = slotsBottom + 8;
                bankRect = flow(bank, null, 22, bankTop);
                bottom = flowBottom + 2;
            }
            case PICK -> {
                optLines = new ArrayList<>();
                optRect = new int[options.size()][];
                int cy = areaTop;
                for (int i = 0; i < options.size(); i++) {
                    List<FormattedCharSequence> ls = this.font.split(Component.literal((char) ('A' + i) + ")  " + options.get(i)), PW - 52);
                    if (ls.size() > 2) ls = ls.subList(0, 2);
                    optLines.add(ls);
                    int h = ls.size() * 10 + 8;
                    optRect[i] = new int[]{16, cy, PW - 32, h};
                    cy += h + 4;
                }
                areaBottom = cy;
                bottom = cy;
            }
            case TYPE -> {
                fieldY = areaTop;
                areaBottom = fieldY + 22;
                bottom = areaBottom;
            }
            default -> { // FIX, GAPTYPE
                shownLines = this.font.split(Component.literal(shown), PW - 32);
                if (shownLines.size() > 3) shownLines = shownLines.subList(0, 3);
                fieldY = areaTop + shownLines.size() * 11 + 6;
                areaBottom = fieldY + 22;
                bottom = areaBottom;
            }
        }
        buttonsY = bottom + 8;
        hintY = buttonsY + 25;
        feedbackY = hintY + 12;
        penaltyY = feedbackY + 22;
        footerY = penaltyY + 12;
        ph = footerY + 16;
    }

    @Override
    protected void init() {
        computeGeo();
        int x = px();
        int y = py();
        if (mode == Exercises.Mode.TYPE || mode == Exercises.Mode.FIX || mode == Exercises.Mode.GAPTYPE) {
            input = new EditBox(this.font, x + 22, y + fieldY + 6, PW - 44, 10, Component.empty());
            input.setBordered(false);
            input.setTextColor(theme.text());
            input.setMaxLength(160);
            addRenderableWidget(input);
            setInitialFocus(input);
        } else {
            input = null;
        }
        if (!shielded) {
            shielded = true;
            Shield.set(true);
        }
        int gap = 5;
        int bw = (PW - 32 - 3 * gap) / 4;
        int by = y + buttonsY;
        int bx = x + 16;
        checkBtn = new ThemedButton(bx, by, bw, 20, checkLabel(), b -> check(), theme);
        addRenderableWidget(checkBtn);
        addRenderableWidget(new ThemedButton(bx + (bw + gap), by, bw, 20,
                Component.translatable("deutschimspiel.button.hint"), b -> hint(), theme));
        addRenderableWidget(new ThemedButton(bx + 2 * (bw + gap), by, bw, 20,
                Component.translatable("deutschimspiel.button.answer"), b -> showAnswer(), theme));
        addRenderableWidget(new ThemedButton(bx + 3 * (bw + gap), by, bw, 20,
                Component.translatable("deutschimspiel.button.close"), b -> onClose(), theme));
    }

    private Component checkLabel() {
        if (!stageDone) return Component.translatable("deutschimspiel.button.check");
        return Component.translatable(cur < stages.size() - 1 ? "deutschimspiel.ex.next" : "deutschimspiel.ex.done");
    }

    // ------------------------------------------------------------ logic

    private void markFailed() {
        if (!failedOnce) {
            failedOnce = true;
            Progress.recordWrong(action, subject.key());
        }
    }

    private String joinedSlots() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < disp.size(); i++) {
            if (i > 0) sb.append(' ');
            sb.append(slotBlank[i] ? (slotFill[i] == null ? "" : slotFill[i]) : disp.get(i));
        }
        return sb.toString();
    }

    private void check() {
        if (stageDone) {
            advance();
            return;
        }
        boolean ok;
        switch (mode) {
            case TYPE, FIX -> {
                String n = Sentences.norm(input.getValue());
                if (n.isEmpty()) return;
                ok = task.accepted().contains(n);
            }
            case GAPTYPE -> {
                String n = Sentences.norm(input.getValue());
                if (n.isEmpty()) return;
                ok = n.equals(Sentences.norm(gapAnswer)) || task.accepted().contains(n);
            }
            case TILES, GAP -> {
                for (int i = 0; i < slotBlank.length; i++) {
                    if (slotBlank[i] && slotFill[i] == null) {
                        feedback = Component.translatable("deutschimspiel.ex.fill_all");
                        feedbackColor = theme.warn();
                        return;
                    }
                }
                ok = task.accepted().contains(Sentences.norm(joinedSlots()));
                if (!ok) {
                    for (int i = 0; i < slotBlank.length; i++) {
                        slotWrong[i] = slotBlank[i] && !Sentences.norm(slotFill[i]).equals(Sentences.norm(disp.get(i)));
                    }
                }
            }
            default -> {
                if (picked < 0) return;
                ok = picked == correctOpt;
                if (!ok) {
                    wrongPicks.add(picked);
                    picked = -1;
                }
            }
        }
        if (ok) {
            stageDone = true;
            feedback = Component.translatable("deutschimspiel.feedback.correct");
            feedbackColor = theme.ok();
            Punishment.onCorrect();
            if (cur == stages.size() - 1) finishSuccess();
            if (checkBtn != null) checkBtn.setMessage(checkLabel());
        } else {
            markFailed();
            feedback = Component.translatable("deutschimspiel.feedback.wrong");
            feedbackColor = theme.bad();
            if (Punishment.onWrong()) {
                List<Penalty> opts = Punishment.available();
                if (!opts.isEmpty()) {
                    Minecraft.getInstance().setScreen(new WheelScreen(opts, false));
                    return;
                }
            }
            penalty = Punishment.canWheel()
                    ? Component.translatable("deutschimspiel.wheel.warning", Punishment.remaining())
                    : Component.empty();
        }
    }

    private void finishSuccess() {
        boolean clean = !failedOnce && !anyHint && !revealed;
        Progress.recordSolved(action, subject.key(), clean);
        if (clean && !Rewards.available().isEmpty() && Progress.countReward(Config.REWARD_EVERY.get())) {
            pendingReward = true;
            penalty = Component.translatable("deutschimspiel.reward.soon");
        }
    }

    private void advance() {
        if (cur < stages.size() - 1) {
            cur++;
            prepareStage(cur);
            this.rebuildWidgets();
        } else {
            onClose();
        }
    }

    private void hint() {
        if (stageDone) return;
        anyHint = true;
        hintLevel = Math.min(2, hintLevel + 1);
    }

    private void showAnswer() {
        if (stageDone) return;
        markFailed();
        revealed = true;
        feedback = Component.translatable("deutschimspiel.feedback.answer", task.primary());
        feedbackColor = theme.warn();
    }

    private void placeTile(int bi) {
        if (bankUsed[bi]) return;
        for (int j = 0; j < slotBlank.length; j++) {
            if (slotBlank[j] && slotFill[j] == null) {
                slotFill[j] = bank.get(bi);
                slotBank[j] = bi;
                bankUsed[bi] = true;
                java.util.Arrays.fill(slotWrong, false);
                return;
            }
        }
    }

    private void unplace(int j) {
        if (slotFill[j] == null) return;
        if (slotBank[j] >= 0) bankUsed[slotBank[j]] = false;
        slotFill[j] = null;
        slotBank[j] = -1;
        java.util.Arrays.fill(slotWrong, false);
    }

    private static boolean hit(int[] r, int ox, int oy, double mx, double my) {
        return mx >= ox + r[0] && mx < ox + r[0] + r[2] && my >= oy + r[1] && my < oy + r[1] + r[3];
    }

    @Override
    public boolean mouseClicked(double mx, double my, int button) {
        int x = px();
        int y = py();
        if (!stageDone) {
            if (mode == Exercises.Mode.TILES || mode == Exercises.Mode.GAP) {
                for (int i = 0; i < bankRect.length; i++) {
                    if (hit(bankRect[i], x, y, mx, my)) {
                        placeTile(i);
                        return true;
                    }
                }
                for (int j = 0; j < slotRect.length; j++) {
                    if (slotBlank[j] && hit(slotRect[j], x, y, mx, my)) {
                        unplace(j);
                        return true;
                    }
                }
            } else if (mode == Exercises.Mode.PICK) {
                for (int i = 0; i < optRect.length; i++) {
                    if (!wrongPicks.contains(i) && hit(optRect[i], x, y, mx, my)) {
                        picked = i;
                        return true;
                    }
                }
            }
        }
        return super.mouseClicked(mx, my, button);
    }

    @Override
    public boolean keyPressed(int key, int scancode, int modifiers) {
        if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) {
            check();
            return true;
        }
        if (key == GLFW.GLFW_KEY_BACKSPACE && !stageDone && (mode == Exercises.Mode.TILES || mode == Exercises.Mode.GAP)) {
            for (int j = slotBlank.length - 1; j >= 0; j--) {
                if (slotBlank[j] && slotFill[j] != null) {
                    unplace(j);
                    break;
                }
            }
            return true;
        }
        return super.keyPressed(key, scancode, modifiers);
    }

    @Override
    public void onClose() {
        if (pendingReward) {
            pendingReward = false;
            List<Reward> rewards = Rewards.available();
            if (!rewards.isEmpty()) {
                Minecraft.getInstance().setScreen(new WheelScreen(rewards, true));
                return;
            }
        }
        super.onClose();
    }

    @Override
    public void removed() {
        if (shielded) {
            shielded = false;
            Shield.set(false);
        }
        super.removed();
    }

    @Override
    public boolean isPauseScreen() {
        return true;
    }

    // ------------------------------------------------------------ drawing

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
        fillRound(g, x - 3, y - 3, PW + 6, ph + 6, 0x55000000);
        fillRound(g, x - 1, y - 1, PW + 2, ph + 2, borderCol);
        fillRound(g, x + 1, y + 1, PW - 2, ph - 2, theme.bg());
        if (theme.id() == Config.ThemeId.PERGAMENT) {
            g.renderOutline(x + 4, y + 4, PW - 8, ph - 8, theme.border2());
        }
        if (theme.id() == Config.ThemeId.MODERN) {
            g.fill(x + 1, y + 1, x + PW - 1, y + 4, theme.accent());
        }
        if (theme.sparkles()) {
            for (int i = 0; i < 24; i++) {
                int sx = x + 6 + (i * 97) % (PW - 12);
                int sy = y + 6 + (i * 53) % (ph - 12);
                int a = (int) (40 + 120 * (0.5 + 0.5 * Math.sin(now / 450.0 + i * 1.7)));
                int c = (a << 24) | 0xE9D5FF;
                g.fill(sx, sy, sx + 1, sy + 1, c);
                if (i % 5 == 0) {
                    g.fill(sx - 1, sy, sx + 2, sy + 1, c);
                    g.fill(sx, sy - 1, sx + 1, sy + 2, c);
                }
            }
        }
        g.fill(x + 16, y + 30, x + 68, y + 82, theme.slot());
        g.renderOutline(x + 16, y + 30, 52, 52, theme.border2());
        if (input != null) {
            g.fill(x + 16, y + fieldY, x + PW - 16, y + fieldY + 22, theme.field());
            g.renderOutline(x + 16, y + fieldY, PW - 32, 22, theme.fieldBorder());
        }
        if (mode == Exercises.Mode.TILES || mode == Exercises.Mode.GAP) {
            g.fill(x + 16, y + areaTop - 2, x + PW - 16, y + areaBottom, theme.field());
            g.renderOutline(x + 16, y + areaTop - 2, PW - 32, areaBottom - areaTop + 2, theme.fieldBorder());
        }
    }

    private void chip(GuiGraphics g, int[] r, String text, int bg, int fg, int outline) {
        int x = px() + r[0];
        int y = py() + r[1];
        if (bg != 0) {
            g.fill(x + 1, y, x + r[2] - 1, y + r[3], bg);
            g.fill(x, y + 1, x + r[2], y + r[3] - 1, bg);
        }
        if (outline != 0) g.renderOutline(x, y, r[2], r[3], outline);
        String t = this.font.plainSubstrByWidth(text, r[2] - 4);
        g.drawString(this.font, t, x + (r[2] - this.font.width(t)) / 2, y + 3, fg, false);
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        int x = px();
        int y = py();

        Component chipText = Component.translatable("deutschimspiel.chip." + action.key);
        int chipW = this.font.width(chipText) + 10;
        int chipTop = theme.id() == Config.ThemeId.MODERN ? y + 12 : y + 11;
        g.fill(x + 16, chipTop, x + 16 + chipW, chipTop + 13, theme.accent());
        g.drawString(this.font, chipText, x + 21, chipTop + 3, theme.accentText(), false);
        Component modeLabel = Component.translatable("deutschimspiel.ex.mode." + mode.name().toLowerCase());
        String stageTxt = stages.size() > 1 ? "  (" + (cur + 1) + "/" + stages.size() + ")" : "";
        g.drawString(this.font, Component.empty().append(modeLabel).append(stageTxt), x + 16 + chipW + 8, chipTop + 3, theme.title(), false);
        String lvl = task.level();
        int lw = this.font.width(lvl) + 8;
        g.fill(x + PW - 16 - lw, chipTop, x + PW - 16, chipTop + 13, theme.border2());
        g.drawString(this.font, lvl, x + PW - 12 - lw, chipTop + 3, theme.accentText(), false);

        g.pose().pushPose();
        g.pose().translate(x + 18, y + 32, 0);
        g.pose().scale(3.0F, 3.0F, 1.0F);
        g.renderItem(subject.icon(), 0, 0);
        g.pose().popPose();

        int ty = y + 32;
        for (int i = 0; i < Math.min(2, promptLines.size()); i++) {
            g.drawString(this.font, promptLines.get(i), x + 76, ty, theme.text(), false);
            ty += 11;
        }
        for (int i = 0; i < Math.min(5, ruLines.size()); i++) {
            g.drawString(this.font, ruLines.get(i), x + 76, ty, lightAccent(), false);
            ty += 11;
        }

        switch (mode) {
            case TILES, GAP -> renderTiles(g, mouseX, mouseY);
            case PICK -> renderPick(g);
            case FIX, GAPTYPE -> {
                for (int i = 0; i < shownLines.size(); i++) {
                    g.drawString(this.font, shownLines.get(i), x + 18, y + areaTop + i * 11,
                            mode == Exercises.Mode.FIX ? theme.warn() : theme.text(), false);
                }
            }
            default -> { }
        }

        if (hintLevel >= 1 && !stageDone) {
            Component h = Component.translatable("deutschimspiel.hint.noun", Sentences.objectDef(subject.word()));
            if (hintLevel >= 2) {
                h = Component.empty().append(h).append("   ")
                        .append(Component.translatable("deutschimspiel.ex.hint_start", startOf(task.primary())));
            }
            List<FormattedCharSequence> hl = this.font.split(h, PW - 32);
            if (!hl.isEmpty()) g.drawCenteredString(this.font, hl.get(0), x + PW / 2, y + hintY, theme.hint());
        }

        List<FormattedCharSequence> fl = this.font.split(feedback, PW - 32);
        for (int i = 0; i < Math.min(2, fl.size()); i++) {
            g.drawCenteredString(this.font, fl.get(i), x + PW / 2, y + feedbackY + i * 10, feedbackColor);
        }
        g.drawCenteredString(this.font, penalty, x + PW / 2, y + penaltyY, theme.bad());
        Progress.Data d = Progress.get();
        int streak = d.streak.getOrDefault(Progress.key(action, subject.key()), 0);
        g.drawCenteredString(this.font,
                Component.translatable("deutschimspiel.footer", d.correct, d.wrong, streak),
                x + PW / 2, y + footerY, theme.hint());
    }

    private int lightAccent() {
        return theme.id() == Config.ThemeId.PERGAMENT ? 0xFF8B3A0F : theme.id() == Config.ThemeId.MODERN ? 0xFF7DD3FC : 0xFFC4B5FD;
    }

    private static String startOf(String sentence) {
        String[] p = sentence.split(" ");
        return p.length <= 1 ? p[0] : p[0] + " " + p[1] + " …";
    }

    private void renderTiles(GuiGraphics g, int mx, int my) {
        int x = px();
        int y = py();
        for (int i = 0; i < slotRect.length; i++) {
            int[] r = slotRect[i];
            if (!slotBlank[i]) {
                chip(g, r, disp.get(i), 0, theme.text(), 0);
            } else if (slotFill[i] == null) {
                chip(g, r, "", theme.slot(), theme.text(), theme.accent());
            } else {
                int out = stageDone ? theme.ok() : slotWrong[i] ? theme.bad() : 0;
                chip(g, r, slotFill[i], theme.accent(), theme.accentText(), out);
            }
        }
        for (int i = 0; i < bankRect.length; i++) {
            int[] r = bankRect[i];
            if (bankUsed[i]) {
                chip(g, r, bank.get(i), 0, theme.hint(), theme.fieldBorder());
            } else {
                boolean hov = !stageDone && hit(r, x, y, mx, my);
                chip(g, r, bank.get(i), hov ? theme.btnHover() : theme.btn(), theme.btnText(), hov ? theme.accent() : 0);
            }
        }
    }

    private void renderPick(GuiGraphics g) {
        int x = px();
        int y = py();
        for (int i = 0; i < optRect.length; i++) {
            int[] r = optRect[i];
            int bg = theme.field();
            int out = theme.fieldBorder();
            if (stageDone && i == correctOpt) out = theme.ok();
            else if (wrongPicks.contains(i)) out = theme.bad();
            else if (picked == i) out = theme.accent();
            g.fill(x + r[0], y + r[1], x + r[0] + r[2], y + r[1] + r[3], bg);
            g.renderOutline(x + r[0], y + r[1], r[2], r[3], out);
            List<FormattedCharSequence> ls = optLines.get(i);
            for (int k = 0; k < ls.size(); k++) {
                g.drawString(this.font, ls.get(k), x + r[0] + 6, y + r[1] + 4 + k * 10,
                        wrongPicks.contains(i) ? theme.hint() : theme.text(), false);
            }
        }
    }
}
