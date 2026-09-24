package com.apocollis.aqtweaks.reskillable;

import com.wdcftgg.farmersdelightlegacy.common.block.BlockTomatoVine;
import net.minecraft.block.BlockCrops;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import rustic.common.blocks.BlockLeavesRustic;
import rustic.common.blocks.crops.BlockGrapeLeaves;
import rustic.common.blocks.crops.BlockLeavesApple;
import xy177.extradelightlegacy.common.block.BlockOrchardLeaves;

import java.util.List;

/** Ripe tree fruit for Orchard. Right-click harvests do not fire {@code HarvestDropsEvent}. */
public final class OrchardFruit {

    private OrchardFruit() {}

    public static ItemStack bonus(World world, BlockPos pos, IBlockState state) {
        if (state == null) return ItemStack.EMPTY;
        if (state.getBlock() instanceof BlockLeavesApple apple) {
            if (state.getValue(BlockLeavesApple.AGE) < apple.getMaxAge()) return ItemStack.EMPTY;
            return new ItemStack(Items.APPLE);
        }
        if (state.getBlock() instanceof BlockGrapeLeaves) {
            if (!state.getValue(BlockGrapeLeaves.GRAPES)) return ItemStack.EMPTY;
            return firstDrop(state.getBlock().getDrops(world, pos, state, 0));
        }
        if (state.getBlock() instanceof BlockLeavesRustic) {
            return firstDrop(state.getBlock().getDrops(world, pos, state, 0));
        }
        if (state.getBlock() instanceof BlockOrchardLeaves) {
            return firstDrop(state.getBlock().getDrops(world, pos, state, 0));
        }
        if (state.getBlock() instanceof BlockTomatoVine vine) {
            if (vine instanceof BlockCrops crops && !crops.isMaxAge(state)) return ItemStack.EMPTY;
            return firstDrop(state.getBlock().getDrops(world, pos, state, 0));
        }
        return ItemStack.EMPTY;
    }

    public static void give(EntityPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) return;
        if (!player.inventory.addItemStackToInventory(stack)) {
            player.dropItem(stack, false);
        }
    }

    private static ItemStack firstDrop(List<ItemStack> drops) {
        if (drops == null) return ItemStack.EMPTY;
        for (ItemStack stack : drops) {
            if (stack != null && !stack.isEmpty()) {
                ItemStack one = stack.copy();
                one.setCount(1);
                return one;
            }
        }
        return ItemStack.EMPTY;
    }
}
