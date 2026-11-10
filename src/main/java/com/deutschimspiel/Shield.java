package com.deutschimspiel;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.network.PacketDistributor;

/** While one of our windows is open the world protects the player from damage. */
public final class Shield {
    private Shield() {}

    public static void set(boolean open) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() == null) return;
        PacketDistributor.sendToServer(new Net.PromptState(open));
    }
}
