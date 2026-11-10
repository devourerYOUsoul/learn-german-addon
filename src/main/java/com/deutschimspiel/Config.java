package com.deutschimspiel;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class Config {
    public enum Tense { PRESENT, PERFECT }

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue SHOW_TOOLTIPS;
    public static final ModConfigSpec.BooleanValue PROMPTS_ENABLED;
    public static final ModConfigSpec.EnumValue<Tense> TENSE;
    public static final ModConfigSpec.IntValue COOLDOWN_SECONDS;
    public static final ModConfigSpec.IntValue CHANCE_PERCENT;
    public static final ModConfigSpec.IntValue MASTERED_AFTER;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        SHOW_TOOLTIPS = b.comment("Show the German name under the item name in tooltips.")
                .define("showTooltips", true);
        PROMPTS_ENABLED = b.comment("Open the 'write the sentence' window after actions (eating, crafting, smelting).")
                .define("promptsEnabled", true);
        TENSE = b.comment("PRESENT = 'Ich esse den Apfel', PERFECT = 'Ich habe den Apfel gegessen'.")
                .defineEnum("tense", Tense.PRESENT);
        COOLDOWN_SECONDS = b.comment("Minimum seconds between two prompts.")
                .defineInRange("cooldownSeconds", 60, 0, 3600);
        CHANCE_PERCENT = b.comment("Chance (percent) that an action opens a prompt once the cooldown is over.")
                .defineInRange("chancePercent", 100, 0, 100);
        MASTERED_AFTER = b.comment("After this many clean answers in a row for the same action+item, it is asked rarely.")
                .defineInRange("masteredAfter", 3, 1, 50);
        SPEC = b.build();
    }

    private Config() {}
}
