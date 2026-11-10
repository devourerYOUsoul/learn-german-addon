package com.deutschimspiel;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Once per game start: writes every item without a German word to config/deutschimspiel-items.txt. */
public final class ItemDump {
    private static boolean done = false;

    private ItemDump() {}

    public static void tick(Minecraft mc) {
        if (done || mc.player == null || mc.level == null) return;
        done = true;
        try {
            List<String> lines = new ArrayList<>();
            for (Item item : BuiltInRegistries.ITEM) {
                ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
                if (id.getPath().equals("air")) continue;
                if (GermanWords.known(id)) continue;
                lines.add(id + " | " + new ItemStack(item).getHoverName().getString());
            }
            Collections.sort(lines);
            Files.write(FMLPaths.CONFIGDIR.get().resolve("deutschimspiel-items.txt"), lines, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
            // not critical
        }
    }
}
