package com.deutschimspiel;

import net.minecraft.world.item.Item;

/** One slice of a wheel: a penalty or a reward. */
public interface WheelItem {
    String key();

    Item icon();

    int color();

    /** Translation key of the display name. */
    String nameKey();
}
