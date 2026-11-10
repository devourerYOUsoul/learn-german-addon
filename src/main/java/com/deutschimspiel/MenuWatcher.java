package com.deutschimspiel;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.BrewingStandMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.inventory.GrindstoneMenu;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.LoomMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.inventory.StonecutterMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Watches open workstation menus on the client and notices when the player takes a result. */
public final class MenuWatcher {
    private static AbstractContainerMenu current;
    private static ItemStack lastResult = ItemStack.EMPTY;
    private static int lastTotal = 0;
    private static ItemStack lastEnchSlot = ItemStack.EMPTY;
    private static int lastBrewTicks = 0;

    private MenuWatcher() {}

    public static void tick(Minecraft mc) {
        Player p = mc.player;
        if (p == null || mc.level == null) {
            current = null;
            return;
        }
        AbstractContainerMenu menu = p.containerMenu;
        if (menu == null || menu instanceof InventoryMenu) {
            current = null;
            return;
        }
        if (menu != current) {
            current = menu;
            lastResult = ItemStack.EMPTY;
            lastTotal = 0;
            lastEnchSlot = ItemStack.EMPTY;
            lastBrewTicks = 0;
        }
        if (!Config.BLOCK_ACTIONS.get()) return;

        if (menu instanceof EnchantmentMenu) {
            watchEnchant(menu);
        } else if (menu instanceof BrewingStandMenu b) {
            watchBrew(b);
        } else {
            Action a = null;
            int slot = -1;
            if (menu instanceof StonecutterMenu) { a = Action.STONECUT; slot = 1; }
            else if (menu instanceof SmithingMenu) { a = Action.SMITH; slot = 3; }
            else if (menu instanceof LoomMenu) { a = Action.WEAVE; slot = 3; }
            else if (menu instanceof CartographyTableMenu) { a = Action.MAP; slot = 2; }
            else if (menu instanceof GrindstoneMenu) { a = Action.GRIND; slot = 2; }
            else if (menu instanceof AnvilMenu) { a = Action.ANVIL; slot = 2; }
            else if (menu instanceof MerchantMenu) { a = Action.TRADE; slot = 2; }
            if (a != null && slot < menu.slots.size()) watchResult(p, menu, a, slot);
        }
    }

    private static int total(Player p, AbstractContainerMenu menu, Item item) {
        int n = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack s = p.getInventory().getItem(i);
            if (s.is(item)) n += s.getCount();
        }
        ItemStack carried = menu.getCarried();
        if (carried.is(item)) n += carried.getCount();
        return n;
    }

    private static void watchResult(Player p, AbstractContainerMenu menu, Action action, int slot) {
        ItemStack cur = menu.getSlot(slot).getItem();
        if (!lastResult.isEmpty()) {
            int now = total(p, menu, lastResult.getItem());
            if (now >= lastTotal + lastResult.getCount()) {
                PromptManager.offer(action, Subject.ofItem(lastResult));
                lastResult = ItemStack.EMPTY;
                lastTotal = 0;
            }
        }
        if (!cur.isEmpty()) {
            lastResult = cur.copy();
            lastTotal = total(p, menu, cur.getItem());
        } else if (!lastResult.isEmpty()) {
            // result vanished without the count growing (inputs changed): forget it
            int now = total(p, menu, lastResult.getItem());
            if (now < lastTotal + lastResult.getCount()) {
                lastResult = ItemStack.EMPTY;
                lastTotal = 0;
            }
        }
    }

    private static void watchEnchant(AbstractContainerMenu menu) {
        ItemStack cur = menu.getSlot(0).getItem();
        if (!lastEnchSlot.isEmpty() && !cur.isEmpty() && !lastEnchSlot.isEnchanted()
                && (cur.isEnchanted() || cur.is(Items.ENCHANTED_BOOK))
                && (cur.is(lastEnchSlot.getItem()) || lastEnchSlot.is(Items.BOOK))) {
            PromptManager.offer(Action.ENCHANT, Subject.ofItem(cur));
        }
        lastEnchSlot = cur.copy();
    }

    private static void watchBrew(BrewingStandMenu menu) {
        int ticks = menu.getBrewingTicks();
        if (lastBrewTicks > 0 && lastBrewTicks <= 20 && ticks == 0) {
            for (int i = 0; i < 3; i++) {
                ItemStack s = menu.getSlot(i).getItem();
                if (!s.isEmpty()) {
                    PromptManager.offer(Action.BREW, Subject.ofItem(s));
                    break;
                }
            }
        }
        lastBrewTicks = ticks;
    }
}
