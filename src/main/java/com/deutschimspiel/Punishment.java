package com.deutschimspiel;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Client side of the wheel: counts wrong answers and asks the world (server) to apply the penalty. */
public final class Punishment {
    private static int wrongInRow = 0;

    private Punishment() {}

    public static boolean canWheel() {
        Minecraft mc = Minecraft.getInstance();
        return Config.PUNISH_ENABLED.get() && mc.player != null && mc.getConnection() != null;
    }

    public static void onCorrect() {
        wrongInRow = 0;
        Minecraft mc = Minecraft.getInstance();
        if (Config.HEAL_PER_ANSWER.get() && mc.player != null && mc.getConnection() != null) {
            PacketDistributor.sendToServer(new Net.Heal());
        }
    }

    /** Button "reset errors": stats counter and the wheel counter go back to zero. */
    public static void resetErrors() {
        wrongInRow = 0;
        Progress.resetErrors();
    }

    /** @return true when the wheel should open now. */
    public static boolean onWrong() {
        if (!canWheel()) return false;
        if (++wrongInRow < Config.WHEEL_AFTER.get()) return false;
        wrongInRow = 0;
        return true;
    }

    public static int remaining() {
        return Math.max(1, Config.WHEEL_AFTER.get() - wrongInRow);
    }

    public static List<Penalty> available() {
        Minecraft mc = Minecraft.getInstance();
        boolean hardcore = mc.level != null && mc.level.getLevelData().isHardcore();
        List<Penalty> out = new ArrayList<>();
        for (Penalty p : Penalty.values()) {
            if (!Config.PENALTY_ON.get(p.key).get()) continue;
            if (hardcore && p.dangerous) continue;
            out.add(p);
        }
        return out;
    }

    public static void apply(Penalty p) {
        if (Minecraft.getInstance().player == null) return;
        PacketDistributor.sendToServer(new Net.PunishRequest(p.key,
                Config.CREEPER_COUNT.get(), Config.VILLAGER_COUNT.get(), Config.PUMPKIN_SECONDS.get(),
                Config.POISON_SECONDS.get(), Config.DAMAGE_HP.get(), Config.LAUNCH_MIN_HEALTH.get(),
                Config.REMOVE_WHOLE_STACK.get()));
    }
}
