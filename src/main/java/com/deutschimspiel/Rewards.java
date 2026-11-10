package com.deutschimspiel;

import net.neoforged.neoforge.network.PacketDistributor;

import java.util.ArrayList;
import java.util.List;

/** Client side of the reward wheel. */
public final class Rewards {
    private Rewards() {}

    public static List<Reward> available() {
        List<Reward> out = new ArrayList<>();
        if (!Config.REWARDS_ENABLED.get()) return out;
        for (Reward r : Reward.values()) {
            if (Config.REWARD_ON.get(r.key()).get()) out.add(r);
        }
        return out;
    }

    public static void claim(Reward r) {
        PacketDistributor.sendToServer(new Net.RewardRequest(r.key()));
    }
}
