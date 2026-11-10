package com.deutschimspiel;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.function.Supplier;

/** All punishments that can land on the wheel. */
public enum Penalty {
    POISON("poison", () -> Items.SPIDER_EYE, 0xFF3FA34D, false),
    DAMAGE("damage", () -> Items.IRON_SWORD, 0xFFD64545, false),
    LAUNCH("launch", () -> Items.FIREWORK_ROCKET, 0xFF3AA6E0, true),
    CREEPERS("creepers", () -> Items.CREEPER_HEAD, 0xFF1FB5A3, true),
    LIGHTNING("lightning", () -> Items.LIGHTNING_ROD, 0xFFF2C94C, true),
    LOSER("loser", () -> Items.OAK_SIGN, 0xFFD94BC4, false),
    VILLAGERS("villagers", () -> Items.IRON_AXE, 0xFF8E5A3C, true),
    PUMPKIN("pumpkin", () -> Items.CARVED_PUMPKIN, 0xFFF2994A, false),
    HUNGER("hunger", () -> Items.ROTTEN_FLESH, 0xFF7A8F2A, false),
    LOSE_ITEM("lose_item", () -> Items.HOPPER, 0xFF7B5BD6, false),
    BREAK_ARMOR("break_armor", () -> Items.IRON_CHESTPLATE, 0xFF6B7C93, false);

    public final String key;
    public final Supplier<Item> icon;
    public final int color;
    /** Skipped in Hardcore worlds. */
    public final boolean dangerous;

    public static Penalty byKey(String key) {
        for (Penalty p : values()) {
            if (p.key.equals(key)) return p;
        }
        return null;
    }

    Penalty(String key, Supplier<Item> icon, int color, boolean dangerous) {
        this.key = key;
        this.icon = icon;
        this.color = color;
        this.dangerous = dangerous;
    }
}
