package com.deutschimspiel;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** What a sentence is about: an item, a mob or a block, with its German word, Russian name and icon. */
public record Subject(ItemStack icon, Component name, GermanWords.Word word, boolean food, String key) {

    public static Subject ofItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        GermanWords.Word w = GermanWords.lookup(stack);
        if (w == null) return null;
        String key = BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath();
        return new Subject(stack.copyWithCount(1), stack.getHoverName(), w, stack.has(DataComponents.FOOD), key);
    }

    public static Subject ofEntity(EntityType<?> type) {
        if (type == null) return null;
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        GermanWords.Word w = GermanWords.lookupEntity(id);
        if (w == null) return null;
        SpawnEggItem egg = SpawnEggItem.byId(type);
        ItemStack icon = egg != null ? new ItemStack(egg) : new ItemStack(Items.NAME_TAG);
        return new Subject(icon, type.getDescription(), w, false, "entity:" + id.getPath());
    }

    public static Subject ofBlock(BlockState state) {
        if (state == null) return null;
        Block block = state.getBlock();
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(block);
        GermanWords.Word w = GermanWords.lookupId(id);
        if (w == null) return null;
        Item item = block.asItem();
        ItemStack icon = item == Items.AIR ? new ItemStack(Items.WHEAT) : new ItemStack(item);
        return new Subject(icon, block.getName(), w, false, "block:" + id.getPath());
    }
}
