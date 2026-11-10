package com.deutschimspiel;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/** All rewards that can land on the reward wheel (every N correct answers). */
public enum Reward implements WheelItem {
    MEAT("meat", () -> Items.COOKED_BEEF, 0xFFC96A3B),
    DIAMOND_PICKAXE("diamond_pickaxe", () -> Items.DIAMOND_PICKAXE, 0xFF3FD4D4),
    DIAMONDS("diamonds", () -> Items.DIAMOND, 0xFF4AC6F0),
    IRON_ARMOR("iron_armor", () -> Items.IRON_CHESTPLATE, 0xFF8A98AD),
    ENCHANTED_BOOK("enchanted_book", () -> Items.ENCHANTED_BOOK, 0xFF9B6BE0),
    EMERALDS("emeralds", () -> Items.EMERALD, 0xFF2EC060),
    GOLDEN_APPLES("golden_apples", () -> Items.GOLDEN_APPLE, 0xFFF2C94C),
    FISHING_ROD("fishing_rod", () -> Items.FISHING_ROD, 0xFF3A8FD6),
    IRON_TOOL("iron_tool", () -> Items.IRON_PICKAXE, 0xFFB8B8C8);

    private final String key;
    private final Supplier<Item> icon;
    private final int color;

    Reward(String key, Supplier<Item> icon, int color) {
        this.key = key;
        this.icon = icon;
        this.color = color;
    }

    public static Reward byKey(String key) {
        for (Reward r : values()) {
            if (r.key.equals(key)) return r;
        }
        return null;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public Item icon() {
        return icon.get();
    }

    @Override
    public int color() {
        return color;
    }

    @Override
    public String nameKey() {
        return "deutschimspiel.reward." + key + ".name";
    }
}
