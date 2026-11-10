package com.deutschimspiel;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Client-side detection of "the player broke a block": remembers the block being hit and notices when it changes. */
public final class BlockWatcher {
    private static BlockPos pos;
    private static BlockState state;
    private static int idleTicks;

    private BlockWatcher() {}

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || !Config.BLOCK_ACTIONS.get()) {
            pos = null;
            state = null;
            return;
        }
        if (pos != null) {
            BlockState now = mc.level.getBlockState(pos);
            if (now.getBlock() != state.getBlock()) {
                BlockState broken = state;
                pos = null;
                state = null;
                onBroken(broken);
            }
        }
        if (mc.options.keyAttack.isDown()
                && mc.hitResult instanceof BlockHitResult hit
                && hit.getType() == HitResult.Type.BLOCK) {
            idleTicks = 0;
            BlockPos p = hit.getBlockPos();
            BlockState s = mc.level.getBlockState(p);
            if (!s.isAir() && (pos == null || !pos.equals(p))) {
                pos = p.immutable();
                state = s;
            }
        } else if (pos != null && ++idleTicks > 20) {
            pos = null;
            state = null;
        }
    }

    private static void onBroken(BlockState broken) {
        ItemStack item = new ItemStack(broken.getBlock().asItem());
        if (item.isEmpty()) return;
        if (broken.is(BlockTags.LOGS)) {
            PromptManager.offer(Action.CHOP, item);
        } else if (broken.is(BlockTags.MINEABLE_WITH_PICKAXE)) {
            PromptManager.offer(Action.MINE, item);
        } else if (broken.is(BlockTags.MINEABLE_WITH_SHOVEL)) {
            PromptManager.offer(Action.DIG, item);
        }
    }
}
