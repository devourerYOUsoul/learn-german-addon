package com.deutschimspiel;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Handlers for messages that arrive on the client. */
public final class ClientHandlers {
    private ClientHandlers() {}

    public static void onFish(Net.FishNotice msg, IPayloadContext ctx) {
        ResourceLocation id = ResourceLocation.tryParse(msg.itemId());
        if (id == null) return;
        Item item = BuiltInRegistries.ITEM.get(id);
        PromptManager.offer(Action.FISH, Subject.ofItem(new ItemStack(item)));
    }
}
