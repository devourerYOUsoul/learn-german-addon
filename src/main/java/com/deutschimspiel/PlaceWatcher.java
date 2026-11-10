package com.deutschimspiel;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.LinkedHashMap;
import java.util.Map;

/** After a right-click with a block item: watches a few ticks whether a block of that item appeared (torch floor/wall, plants). */
public final class PlaceWatcher {
    private static final Map<BlockPos, BlockState> before = new LinkedHashMap<>();
    private static ItemStack held = ItemStack.EMPTY;
    private static int ticksLeft = 0;

    private PlaceWatcher() {}

    public static void record(Level level, BlockPos clicked, Direction face, ItemStack stack) {
        if (stack == null || stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) return;
        before.clear();
        before.put(clicked.immutable(), level.getBlockState(clicked));
        BlockPos rel = clicked.relative(face).immutable();
        before.put(rel, level.getBlockState(rel));
        held = stack.copyWithCount(1);
        ticksLeft = 8;
    }

    public static void tick(Minecraft mc) {
        if (ticksLeft <= 0) return;
        if (mc.level == null || mc.player == null) {
            ticksLeft = 0;
            return;
        }
        ticksLeft--;
        for (Map.Entry<BlockPos, BlockState> e : before.entrySet()) {
            BlockState now = mc.level.getBlockState(e.getKey());
            if (now == e.getValue() || now.isAir()) continue;
            if (now.getBlock().asItem() != held.getItem()) continue;
            ticksLeft = 0;
            onPlaced(now);
            return;
        }
    }

    private static void onPlaced(BlockState now) {
        String itemPath = BuiltInRegistries.ITEM.getKey(held.getItem()).getPath();
        String blockPath = BuiltInRegistries.BLOCK.getKey(now.getBlock()).getPath();
        if (itemPath.contains("torch")) {
            PromptManager.offer(blockPath.contains("wall") ? Action.PLACE_WALL : Action.PLACE_FLOOR, Subject.ofItem(held));
        } else if (now.is(BlockTags.CROPS) || now.is(BlockTags.SAPLINGS) || now.is(BlockTags.FLOWERS)) {
            PromptManager.offer(Action.PLANT, Subject.ofItem(held));
        }
    }
}
