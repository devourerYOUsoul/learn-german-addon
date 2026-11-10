package com.deutschimspiel;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import java.util.EnumMap;
import java.util.Map;
import java.util.Random;

/** Decides when a sentence prompt opens. The screen opens only when no other screen is open. */
public final class PromptManager {
    private record Pending(Action action, Subject subject) {}

    private static final Random RNG = new Random();
    private static final Map<Action, Long> LAST_BY_ACTION = new EnumMap<>(Action.class);
    private static Pending pending;
    private static long lastPromptTime = 0L;

    private PromptManager() {}

    public static void offer(Action action, ItemStack stack) {
        offer(action, Subject.ofItem(stack));
    }

    public static void offer(Action action, Subject subject) {
        if (subject == null || pending != null || !Config.PROMPTS_ENABLED.get()) return;
        if (!Config.ACTION_ON.get(action.key).get()) return;
        long now = System.currentTimeMillis();
        if (now - lastPromptTime < Config.COOLDOWN_SECONDS.get() * 1000L) return;
        if (action.combat && now - LAST_BY_ACTION.getOrDefault(action, 0L) < Config.COMBAT_COOLDOWN_SECONDS.get() * 1000L) return;
        if (RNG.nextInt(100) >= Config.CHANCE_PERCENT.get()) return;
        int streak = Progress.get().streak.getOrDefault(Progress.key(action, subject.key()), 0);
        if (streak >= Config.MASTERED_AFTER.get() && RNG.nextInt(100) >= 15) return;
        pending = new Pending(action, subject);
    }

    public static void tick(Minecraft mc) {
        if (pending == null || mc.player == null || mc.level == null || mc.screen != null) return;
        Pending p = pending;
        pending = null;
        long now = System.currentTimeMillis();
        lastPromptTime = now;
        LAST_BY_ACTION.put(p.action(), now);
        mc.setScreen(new SentenceScreen(p.action(), p.subject()));
    }
}
