package com.deutschimspiel;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.Random;

/** Decides when a sentence prompt opens. The screen opens only when no other screen is open. */
public final class PromptManager {
    private record Pending(Action action, ItemStack stack, GermanWords.Word word) {}

    private static final Random RNG = new Random();
    private static Pending pending;
    private static long lastPromptTime = 0L;

    private PromptManager() {}

    public static void offer(Action action, ItemStack stack) {
        if (!Config.PROMPTS_ENABLED.get() || stack == null || stack.isEmpty()) return;
        GermanWords.Word w = GermanWords.lookup(stack);
        if (w == null) return;
        long now = System.currentTimeMillis();
        if (now - lastPromptTime < Config.COOLDOWN_SECONDS.get() * 1000L) return;
        if (RNG.nextInt(100) >= Config.CHANCE_PERCENT.get()) return;
        int streak = Progress.get().streak.getOrDefault(Progress.key(action, stack), 0);
        if (streak >= Config.MASTERED_AFTER.get() && RNG.nextInt(100) >= 15) return;
        pending = new Pending(action, stack.copyWithCount(1), w);
    }

    public static void tick(Minecraft mc) {
        if (pending == null || mc.player == null || mc.level == null || mc.screen != null) return;
        Pending p = pending;
        pending = null;
        lastPromptTime = System.currentTimeMillis();
        mc.setScreen(new SentenceScreen(p.action(), p.stack(), p.word()));
    }
}
