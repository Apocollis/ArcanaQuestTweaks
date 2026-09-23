package com.apocollis.aqtweaks.aether;

import com.gildedgames.the_aether.blocks.BlocksAether;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public final class AetherPortalIsland {

    private AetherPortalIsland() {
    }

    public static boolean isIslandGround(IBlockState state) {
        Block block = state.getBlock();
        return block == BlocksAether.aether_grass
                || block == BlocksAether.enchanted_aether_grass
                || block == BlocksAether.aether_dirt
                || block == BlocksAether.holystone
                || block == BlocksAether.mossy_holystone;
    }

    /**
     * Top island-ground Y in the column, walking down through air and canopy.
     * {@code Integer.MIN_VALUE} if the column is void.
     */
    public static int findSurfaceY(World world, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = world.getActualHeight() - 1; y >= 1; y--) {
            pos.setPos(x, y, z);
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            if (world.isAirBlock(pos)) {
                continue;
            }
            if (block.isLeaves(state, world, pos) || block.isWood(world, pos) || block.isFoliage(world, pos)) {
                continue;
            }
            if (isIslandGround(state)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    public static boolean hasIslandColumn(World world, int x, int z) {
        return findSurfaceY(world, x, z) != Integer.MIN_VALUE;
    }

    public static boolean platformIsIsland(World world, int originX, int platformY, int originZ,
            int platformWidth, int platformLength, boolean verticalZ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int widthOffset = 0; widthOffset < platformWidth; widthOffset++) {
            for (int lengthOffset = 0; lengthOffset < platformLength; lengthOffset++) {
                int ox = verticalZ ? lengthOffset : widthOffset;
                int oz = verticalZ ? widthOffset : lengthOffset;
                if (!isIslandGround(world.getBlockState(pos.setPos(originX + ox, platformY, originZ + oz)))) {
                    return false;
                }
            }
        }
        return true;
    }
}
