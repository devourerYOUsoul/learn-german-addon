package com.deutschimspiel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Common part (runs on the host's world and on dedicated servers): receives punishment requests
 * from players and tells the fisher's client about a catch. The client part is DeutschImSpielClient.
 */
@Mod(DeutschImSpiel.MODID)
public class DeutschImSpiel {
    public static final String MODID = "deutschimspiel";

    public DeutschImSpiel(IEventBus modBus) {
        modBus.addListener(Net::register);
        NeoForge.EVENT_BUS.addListener(ServerPunish::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(ServerPunish::onLogout);
        NeoForge.EVENT_BUS.addListener(ServerPunish::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(this::onFished);
    }

    private void onFished(ItemFishedEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp) || e.getDrops().isEmpty()) return;
        ItemStack first = e.getDrops().get(0);
        if (first.isEmpty()) return;
        PacketDistributor.sendToPlayer(sp, new Net.FishNotice(BuiltInRegistries.ITEM.getKey(first.getItem()).toString()));
    }
}
