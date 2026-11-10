package com.deutschimspiel;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public enum Tense { PRESENT, PERFECT }
    public enum ThemeId { PERGAMENT, MAGIE, MODERN }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_TOOLTIPS;
    public static final ModConfigSpec.BooleanValue SHOW_LOOKAT;
    public static final ModConfigSpec.IntValue LOOK_X;
    public static final ModConfigSpec.IntValue LOOK_Y;
    public static final ModConfigSpec.BooleanValue PROMPTS_ENABLED;
    public static final ModConfigSpec.BooleanValue BLOCK_ACTIONS;
    public static final ModConfigSpec.IntValue COMBAT_COOLDOWN_SECONDS;
    public static final String[] ACTION_KEYS = {"eat", "drink", "craft", "smelt", "chop", "mine", "dig", "enchant", "anvil",
            "brew", "smith", "stonecut", "weave", "map", "grind", "trade", "fish", "feed", "plant", "harvest",
            "hit", "shoot", "block", "place_floor", "place_wall"};
    public static final java.util.Map<String, ModConfigSpec.BooleanValue> ACTION_ON = new java.util.HashMap<>();
    public static final ModConfigSpec.EnumValue<Tense> TENSE;
    public static final ModConfigSpec.EnumValue<ThemeId> THEME;
    public static final ModConfigSpec.IntValue COOLDOWN_SECONDS;
    public static final ModConfigSpec.IntValue CHANCE_PERCENT;
    public static final ModConfigSpec.IntValue MASTERED_AFTER;

    public static final ModConfigSpec.BooleanValue PUNISH_ENABLED;
    public static final ModConfigSpec.IntValue WHEEL_AFTER;
    public static final java.util.Map<String, ModConfigSpec.BooleanValue> PENALTY_ON = new java.util.HashMap<>();
    public static final String[] PENALTY_KEYS = {"poison", "damage", "launch", "creepers", "lightning", "loser",
            "villagers", "pumpkin", "hunger", "lose_item", "break_armor"};
    public static final ModConfigSpec.BooleanValue REWARDS_ENABLED;
    public static final ModConfigSpec.IntValue REWARD_EVERY;
    public static final String[] REWARD_KEYS = {"meat", "diamond_pickaxe", "diamonds", "iron_armor", "enchanted_book",
            "emeralds", "golden_apples", "fishing_rod", "iron_tool"};
    public static final java.util.Map<String, ModConfigSpec.BooleanValue> REWARD_ON = new java.util.HashMap<>();
    public static final ModConfigSpec.IntValue CREEPER_COUNT;
    public static final ModConfigSpec.IntValue VILLAGER_COUNT;
    public static final ModConfigSpec.IntValue PUMPKIN_SECONDS;
    public static final ModConfigSpec.BooleanValue REMOVE_WHOLE_STACK;
    public static final ModConfigSpec.IntValue POISON_SECONDS;
    public static final ModConfigSpec.IntValue DAMAGE_HP;
    public static final ModConfigSpec.IntValue LAUNCH_MIN_HEALTH;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SHOW_TOOLTIPS = b.comment("Show the German name under the item name in tooltips.")
                .define("showTooltips", true);
        SHOW_LOOKAT = b.comment("Show the German name of the block / dropped item you are looking at (small panel).")
                .define("showLookAt", true);
        LOOK_X = b.comment("Horizontal offset of the look-at panel from the screen centre (GUI pixels).")
                .defineInRange("lookAtX", 0, -800, 800);
        LOOK_Y = b.comment("Distance from the top of the screen (GUI pixels). Raise it if it overlaps Jade.")
                .defineInRange("lookAtY", 44, 0, 800);
        PROMPTS_ENABLED = b.comment("Open the 'write the sentence' window after actions.")
                .define("promptsEnabled", true);
        BLOCK_ACTIONS = b.comment("Also ask after breaking blocks (chopping wood, mining, digging).")
                .define("blockActions", true);
        COMBAT_COOLDOWN_SECONDS = b.comment("Extra cooldown (seconds) for hits, bow shots and shield blocks, so they do not get annoying.")
                .defineInRange("combatCooldownSeconds", 240, 0, 7200);
        b.push("actions");
        for (String k : ACTION_KEYS) {
            ACTION_ON.put(k, b.define(k, true));
        }
        b.pop();
        TENSE = b.comment("PRESENT = 'Ich esse den Apfel', PERFECT = 'Ich habe den Apfel gegessen'.")
                .defineEnum("tense", Tense.PRESENT);
        THEME = b.comment("Look of the window: PERGAMENT (parchment), MAGIE (arcane), MODERN (dark glass).")
                .defineEnum("theme", ThemeId.MAGIE);
        COOLDOWN_SECONDS = b.comment("Minimum seconds between two prompts.")
                .defineInRange("cooldownSeconds", 60, 0, 3600);
        CHANCE_PERCENT = b.comment("Chance (percent) that an action opens a prompt once the cooldown is over.")
                .defineInRange("chancePercent", 100, 0, 100);
        MASTERED_AFTER = b.comment("After this many clean answers in a row for the same action+item, it is asked rarely.")
                .defineInRange("masteredAfter", 3, 1, 50);

        b.push("punishments");
        PUNISH_ENABLED = b.comment("Wheel of punishments after wrong answers. Works in single player, LAN and servers (the world applies it; every player needs the mod).")
                .define("enabled", true);
        WHEEL_AFTER = b.comment("The wheel spins after this many wrong answers in a row.")
                .defineInRange("wheelAfterWrong", 2, 1, 20);
        b.push("on");
        for (String k : PENALTY_KEYS) {
            PENALTY_ON.put(k, b.define(k, true));
        }
        b.pop();
        CREEPER_COUNT = b.defineInRange("creeperCount", 3, 1, 10);
        VILLAGER_COUNT = b.comment("Angry villagers = vindicators with axes.")
                .defineInRange("villagerCount", 6, 1, 20);
        PUMPKIN_SECONDS = b.defineInRange("pumpkinSeconds", 300, 10, 3600);
        REMOVE_WHOLE_STACK = b.comment("true = the whole stack of a random inventory slot disappears, false = only one item.")
                .define("removeWholeStack", true);
        POISON_SECONDS = b.defineInRange("poisonSeconds", 6, 1, 60);
        DAMAGE_HP = b.comment("Half-hearts of damage (4 = two hearts). Never kills you.")
                .defineInRange("damageHp", 4, 1, 19);
        LAUNCH_MIN_HEALTH = b.comment("Launch only if your health is above this value (fall damage!). Otherwise poison is used.")
                .defineInRange("launchMinHealth", 14, 1, 20);
        b.pop();
        b.push("rewards");
        REWARDS_ENABLED = b.comment("Reward wheel after every N correct answers.").define("enabled", true);
        REWARD_EVERY = b.comment("Correct answers (no hints) needed for one spin of the reward wheel.")
                .defineInRange("everyCorrect", 10, 1, 1000);
        b.push("on");
        for (String k : REWARD_KEYS) {
            REWARD_ON.put(k, b.define(k, true));
        }
        b.pop();
        b.pop();
        SPEC = b.build();
    }

    private Config() {}
}
