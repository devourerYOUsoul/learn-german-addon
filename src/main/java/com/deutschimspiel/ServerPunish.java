package com.deutschimspiel;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/** Server side of the wheel punishments: applies a requested penalty to the player who asked for it. */
public final class ServerPunish {
    private static final Random RNG = new Random();
    /** Initial upward speed that lifts the player about 10 blocks (vanilla gravity and drag). */
    private static final double LAUNCH_SPEED = 1.35;

    private static final class PumpkinState {
        final ItemStack previous;
        long endTime;

        PumpkinState(ItemStack previous, long endTime) {
            this.previous = previous;
            this.endTime = endTime;
        }
    }

    private static final Map<UUID, PumpkinState> PUMPKINS = new HashMap<>();

    private ServerPunish() {}

    // ---------- request from a client ----------

    public static void onRequest(Net.PunishRequest r, IPayloadContext ctx) {
        if (!(ctx.player() instanceof ServerPlayer sp) || !sp.isAlive()) return;
        Penalty p = Penalty.byKey(r.key());
        if (p == null) return;
        if (p.dangerous && sp.server.isHardcore()) return;
        apply(sp, p, r);
        sp.server.getPlayerList().broadcastSystemMessage(
                Component.translatable("deutschimspiel.broadcast", sp.getDisplayName(),
                        Component.translatable("deutschimspiel.penalty." + p.key + ".name")
                                .withStyle(ChatFormatting.GOLD)).withStyle(ChatFormatting.YELLOW),
                false);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static void apply(ServerPlayer sp, Penalty p, Net.PunishRequest r) {
        int poison = clamp(r.poisonSeconds(), 1, 60);
        switch (p) {
            case POISON:
                sp.addEffect(new MobEffectInstance(MobEffects.POISON, poison * 20, 0));
                break;
            case DAMAGE: {
                float amount = Math.min((float) clamp(r.damageHp(), 1, 20), sp.getHealth() - 1.0F);
                if (amount > 0) sp.hurt(sp.damageSources().magic(), amount);
                break;
            }
            case LAUNCH:
                if (sp.getHealth() <= clamp(r.launchMinHealth(), 1, 20)) {
                    sp.addEffect(new MobEffectInstance(MobEffects.POISON, poison * 20, 0));
                } else {
                    Vec3 m = sp.getDeltaMovement();
                    sp.setDeltaMovement(m.x, LAUNCH_SPEED, m.z);
                    sp.hurtMarked = true;
                }
                break;
            case CREEPERS:
                spawnAround(sp, EntityType.CREEPER, clamp(r.creepers(), 1, 10), 3.5);
                break;
            case LIGHTNING: {
                ServerLevel level = sp.serverLevel();
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(sp.getX(), sp.getY(), sp.getZ());
                    if (sp.getHealth() <= 8.0F) bolt.setVisualOnly(true);
                    level.addFreshEntity(bolt);
                }
                break;
            }
            case LOSER:
                sp.connection.send(new ClientboundSetTitlesAnimationPacket(5, 80, 15));
                sp.connection.send(new ClientboundSetSubtitleTextPacket(Component.empty()));
                sp.connection.send(new ClientboundSetTitleTextPacket(
                        Component.translatable("deutschimspiel.penalty.loser.title")
                                .withStyle(ChatFormatting.RED, ChatFormatting.BOLD)));
                break;
            case VILLAGERS:
                spawnAround(sp, EntityType.VINDICATOR, clamp(r.villagers(), 1, 12), 4.5);
                break;
            case PUMPKIN:
                putPumpkin(sp, clamp(r.pumpkinSeconds(), 10, 3600));
                break;
            case HUNGER:
                sp.getFoodData().setFoodLevel(0);
                sp.getFoodData().setSaturation(0.0F);
                break;
            case LOSE_ITEM:
                loseItem(sp, r.wholeStack());
                break;
            case BREAK_ARMOR:
                breakArmor(sp);
                break;
            default:
                break;
        }
    }

    private static <T extends Mob> void spawnAround(ServerPlayer sp, EntityType<T> type, int n, double radius) {
        ServerLevel level = sp.serverLevel();
        for (int i = 0; i < n; i++) {
            BlockPos spot = null;
            for (int attempt = 0; attempt < 12 && spot == null; attempt++) {
                double ang = RNG.nextDouble() * Math.PI * 2;
                double r = radius - 1.0 + RNG.nextDouble() * 2.0;
                int bx = (int) Math.floor(sp.getX() + Math.cos(ang) * r);
                int bz = (int) Math.floor(sp.getZ() + Math.sin(ang) * r);
                int by = (int) Math.floor(sp.getY());
                for (int dy : new int[] {0, 1, -1, 2}) {
                    BlockPos feet = new BlockPos(bx, by + dy, bz);
                    if (level.getBlockState(feet).isAir() && level.getBlockState(feet.above()).isAir()) {
                        spot = feet;
                        break;
                    }
                }
            }
            if (spot == null) spot = sp.blockPosition();
            T mob = type.spawn(level, spot, MobSpawnType.EVENT);
            if (mob != null) mob.setTarget(sp);
        }
    }

    private static void putPumpkin(ServerPlayer sp, int seconds) {
        long end = sp.serverLevel().getGameTime() + seconds * 20L;
        PumpkinState st = PUMPKINS.get(sp.getUUID());
        if (st == null) {
            ItemStack prev = sp.getItemBySlot(EquipmentSlot.HEAD);
            st = new PumpkinState(prev.is(Items.CARVED_PUMPKIN) ? ItemStack.EMPTY : prev.copy(), end);
            PUMPKINS.put(sp.getUUID(), st);
        } else {
            st.endTime = end;
        }
        ItemStack pumpkin = new ItemStack(Items.CARVED_PUMPKIN);
        pumpkin.set(DataComponents.CUSTOM_NAME, Component.translatable("deutschimspiel.penalty.pumpkin.item"));
        sp.setItemSlot(EquipmentSlot.HEAD, pumpkin);
    }

    private static void endPumpkin(ServerPlayer sp, PumpkinState st) {
        PUMPKINS.remove(sp.getUUID());
        if (sp.getItemBySlot(EquipmentSlot.HEAD).is(Items.CARVED_PUMPKIN)) {
            sp.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        }
        if (!st.previous.isEmpty()) {
            if (sp.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
                sp.setItemSlot(EquipmentSlot.HEAD, st.previous);
            } else {
                sp.getInventory().placeItemBackInInventory(st.previous);
            }
        }
    }

    public static void onPlayerTick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        PumpkinState st = PUMPKINS.get(sp.getUUID());
        if (st == null) return;
        if (sp.serverLevel().getGameTime() >= st.endTime) {
            endPumpkin(sp, st);
            sp.sendSystemMessage(Component.translatable("deutschimspiel.penalty.pumpkin.end"));
            return;
        }
        ItemStack head = sp.getItemBySlot(EquipmentSlot.HEAD);
        if (!head.is(Items.CARVED_PUMPKIN)) {
            if (!head.isEmpty()) sp.getInventory().placeItemBackInInventory(head.copy());
            ItemStack pumpkin = new ItemStack(Items.CARVED_PUMPKIN);
            pumpkin.set(DataComponents.CUSTOM_NAME, Component.translatable("deutschimspiel.penalty.pumpkin.item"));
            sp.setItemSlot(EquipmentSlot.HEAD, pumpkin);
        }
    }

    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer sp)) return;
        PumpkinState st = PUMPKINS.get(sp.getUUID());
        if (st != null) endPumpkin(sp, st);
    }

    private static void loseItem(ServerPlayer sp, boolean wholeStack) {
        Inventory inv = sp.getInventory();
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 36; i++) {
            if (!inv.getItem(i).isEmpty()) slots.add(i);
        }
        if (slots.isEmpty()) {
            sp.sendSystemMessage(Component.translatable("deutschimspiel.penalty.lose_item.none"));
            return;
        }
        int slot = slots.get(RNG.nextInt(slots.size()));
        ItemStack st = inv.getItem(slot);
        Component name = st.getHoverName();
        if (wholeStack || st.getCount() == 1) {
            inv.setItem(slot, ItemStack.EMPTY);
        } else {
            st.shrink(1);
        }
        sp.sendSystemMessage(Component.translatable("deutschimspiel.penalty.lose_item.msg", name));
    }

    private static void breakArmor(ServerPlayer sp) {
        EquipmentSlot[] all = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        List<EquipmentSlot> worn = new ArrayList<>();
        for (EquipmentSlot s : all) {
            ItemStack it = sp.getItemBySlot(s);
            if (it.isEmpty()) continue;
            if (s == EquipmentSlot.HEAD && PUMPKINS.containsKey(sp.getUUID())) continue;
            worn.add(s);
        }
        if (worn.isEmpty()) {
            sp.sendSystemMessage(Component.translatable("deutschimspiel.penalty.break_armor.none"));
            return;
        }
        EquipmentSlot slot = worn.get(RNG.nextInt(worn.size()));
        Component name = sp.getItemBySlot(slot).getHoverName();
        sp.setItemSlot(slot, ItemStack.EMPTY);
        sp.sendSystemMessage(Component.translatable("deutschimspiel.penalty.break_armor.msg", name));
    }
}
