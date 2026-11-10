package com.deutschimspiel;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

import java.util.List;

/** Client-only addon: German names in tooltips + "write the sentence" prompts. */
@Mod(value = DeutschImSpiel.MODID, dist = Dist.CLIENT)
public class DeutschImSpielClient {
    public DeutschImSpielClient(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.CLIENT, Config.SPEC);
        NeoForge.EVENT_BUS.addListener(this::onTooltip);
        NeoForge.EVENT_BUS.addListener(this::onUseFinish);
        NeoForge.EVENT_BUS.addListener(this::onCrafted);
        NeoForge.EVENT_BUS.addListener(this::onSmelted);
        NeoForge.EVENT_BUS.addListener(this::onTick);
        NeoForge.EVENT_BUS.addListener(this::onAttack);
        NeoForge.EVENT_BUS.addListener(this::onUseStart);
        NeoForge.EVENT_BUS.addListener(this::onUseStop);
        NeoForge.EVENT_BUS.addListener(this::onEntityInteract);
        NeoForge.EVENT_BUS.addListener(this::onRightClickBlock);
        NeoForge.EVENT_BUS.addListener(LookAtOverlay::render);
    }

    private void onTooltip(ItemTooltipEvent e) {
        if (!Config.SHOW_TOOLTIPS.get()) return;
        ItemStack st = e.getItemStack();
        if (st.isEmpty()) return;
        GermanWords.Word w = GermanWords.lookup(st);
        if (w == null) return;
        List<Component> tip = e.getToolTip();
        tip.add(Math.min(1, tip.size()), w.tooltipLine());
    }

    private boolean isLocalClientPlayer(Player p) {
        Minecraft mc = Minecraft.getInstance();
        return p != null && p == mc.player && p.level().isClientSide();
    }

    private void onUseFinish(LivingEntityUseItemEvent.Finish e) {
        if (!(e.getEntity() instanceof Player p) || !isLocalClientPlayer(p)) return;
        ItemStack st = e.getItem();
        if (st.isEmpty()) return;
        if (st.getUseAnimation() == UseAnim.DRINK) {
            PromptManager.offer(Action.DRINK, st);
        } else if (st.has(DataComponents.FOOD)) {
            PromptManager.offer(Action.EAT, st);
        }
    }

    private void onCrafted(PlayerEvent.ItemCraftedEvent e) {
        if (!isLocalClientPlayer(e.getEntity())) return;
        PromptManager.offer(Action.CRAFT, e.getCrafting());
    }

    private void onSmelted(PlayerEvent.ItemSmeltedEvent e) {
        if (!isLocalClientPlayer(e.getEntity())) return;
        PromptManager.offer(Action.SMELT, e.getSmelting());
    }

    private void onTick(ClientTickEvent.Post e) {
        Minecraft mc = Minecraft.getInstance();
        BlockWatcher.tick(mc);
        PlaceWatcher.tick(mc);
        MenuWatcher.tick(mc);
        ItemDump.tick(mc);
        PromptManager.tick(mc);
    }

    private void onAttack(AttackEntityEvent e) {
        if (!isLocalClientPlayer(e.getEntity())) return;
        if (e.getTarget() instanceof LivingEntity le && !(le instanceof Player)) {
            PromptManager.offer(Action.HIT, Subject.ofEntity(le.getType()));
        }
    }

    private void onUseStart(LivingEntityUseItemEvent.Start e) {
        if (!(e.getEntity() instanceof Player p) || !isLocalClientPlayer(p)) return;
        ItemStack st = e.getItem();
        if (st.isEmpty() || st.getUseAnimation() != UseAnim.BLOCK) return;
        if (p.level().getEntitiesOfClass(Monster.class, p.getBoundingBox().inflate(10.0)).isEmpty()) return;
        PromptManager.offer(Action.BLOCK, st);
    }

    private void onUseStop(LivingEntityUseItemEvent.Stop e) {
        if (!(e.getEntity() instanceof Player p) || !isLocalClientPlayer(p)) return;
        ItemStack st = e.getItem();
        if (st.isEmpty() || st.getUseAnimation() != UseAnim.BOW) return;
        if (72000 - e.getDuration() < 10) return;
        if (p.getProjectile(st).isEmpty() && !p.getAbilities().instabuild) return;
        PromptManager.offer(Action.SHOOT, st);
    }

    private void onEntityInteract(PlayerInteractEvent.EntityInteract e) {
        if (!isLocalClientPlayer(e.getEntity())) return;
        if (e.getTarget() instanceof Animal a && a.isFood(e.getItemStack()) && a.canFallInLove() && a.getAge() == 0) {
            PromptManager.offer(Action.FEED, Subject.ofEntity(a.getType()));
        }
    }

    private void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!isLocalClientPlayer(e.getEntity())) return;
        PlaceWatcher.record(e.getLevel(), e.getPos(), e.getFace(), e.getItemStack());
    }
}
