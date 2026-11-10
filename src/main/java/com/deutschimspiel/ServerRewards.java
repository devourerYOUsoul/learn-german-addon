package com.deutschimspiel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Server side of the reward wheel: hands the won item to the player who asked. */
public final class ServerRewards {
    private static final Random RNG = new Random();
    private static final Map<UUID, Long> LAST = new HashMap<>();

    private ServerRewards() {}

    public static void onRequest(Net.RewardRequest msg, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp) || !sp.isAlive()) return;
        Reward r = Reward.byKey(msg.key());
        if (r == null) return;
        long now = sp.serverLevel().getGameTime();
        Long last = LAST.get(sp.getUUID());
        if (last != null && now - last < 600L) return; // at most one reward per 30 seconds
        LAST.put(sp.getUUID(), now);

        ItemStack given = give(sp, r);
        sp.server.getPlayerList().broadcastSystemMessage(
                Component.translatable("deutschimspiel.broadcast.reward", sp.getDisplayName(),
                        Component.translatable(r.nameKey()).withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.GREEN),
                false);
        if (given != null && !given.isEmpty()) {
            sp.sendSystemMessage(Component.translatable("deutschimspiel.reward.got", given.getHoverName()));
        }
    }

    private static ItemStack give(ServerPlayer sp, Reward r) {
        ItemStack st;
        switch (r) {
            case MEAT:
                st = new ItemStack(Items.COOKED_BEEF, 10);
                break;
            case DIAMOND_PICKAXE:
                st = new ItemStack(Items.DIAMOND_PICKAXE);
                break;
            case DIAMONDS:
                st = new ItemStack(Items.DIAMOND, 3);
                break;
            case IRON_ARMOR:
                st = new ItemStack(pick(Items.IRON_HELMET, Items.IRON_CHESTPLATE, Items.IRON_LEGGINGS, Items.IRON_BOOTS));
                break;
            case ENCHANTED_BOOK: {
                st = new ItemStack(Items.ENCHANTED_BOOK);
                enchantRandomly(sp, st, true);
                break;
            }
            case EMERALDS:
                st = new ItemStack(Items.EMERALD, 3);
                break;
            case GOLDEN_APPLES:
                st = new ItemStack(Items.GOLDEN_APPLE, 5);
                break;
            case FISHING_ROD: {
                st = new ItemStack(Items.FISHING_ROD);
                enchantRandomly(sp, st, false);
                break;
            }
            case IRON_TOOL:
                st = new ItemStack(pick(Items.IRON_SWORD, Items.IRON_PICKAXE, Items.IRON_AXE, Items.IRON_SHOVEL, Items.IRON_HOE));
                break;
            default:
                return null;
        }
        ItemStack shown = st.copy();
        if (!sp.getInventory().add(st)) {
            sp.drop(st, false);
        }
        return shown;
    }

    private static Item pick(Item... items) {
        return items[RNG.nextInt(items.length)];
    }

    /** Adds one random non-curse enchantment (any level up to its maximum). */
    private static void enchantRandomly(ServerPlayer sp, ItemStack stack, boolean book) {
        Registry<Enchantment> reg = sp.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
        List<Holder.Reference<Enchantment>> all = reg.holders()
                .filter(h -> !h.is(EnchantmentTags.CURSE))
                .filter(h -> book || h.value().isSupportedItem(stack))
                .toList();
        if (all.isEmpty()) return;
        Holder<Enchantment> pick = all.get(RNG.nextInt(all.size()));
        int level = 1 + RNG.nextInt(Math.max(1, pick.value().getMaxLevel()));
        EnchantmentHelper.updateEnchantments(stack, m -> m.set(pick, level));
    }
}
