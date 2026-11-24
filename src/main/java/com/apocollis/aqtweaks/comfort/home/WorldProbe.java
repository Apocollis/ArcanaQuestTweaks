package com.apocollis.aqtweaks.comfort.home;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.BlockFenceGate;
import net.minecraft.block.BlockTrapDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/** {@link HomeFiller.Probe} over a live world. Never loads a chunk: callers check {@link #loaded} first. */
final class WorldProbe implements HomeFiller.Probe {
    private final World world;
    private final boolean skylight;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    WorldProbe(World world) {
        this.world = world;
        this.skylight = world.provider.hasSkyLight();
    }

    @Override
    public boolean loaded(int x, int y, int z) {
        cursor.setPos(x, y, z);
        return world.isBlockLoaded(cursor);
    }

    @Override
    public boolean passable(int x, int y, int z) {
        cursor.setPos(x, y, z);
        IBlockState state = world.getBlockState(cursor);
        AxisAlignedBB box = state.getCollisionBoundingBox(world, cursor);
        return box == null || box == Block.NULL_AABB;
    }

    @Override
    public boolean portal(int x, int y, int z) {
        cursor.setPos(x, y, z);
        Block block = world.getBlockState(cursor).getBlock();
        return block instanceof BlockDoor || block instanceof BlockTrapDoor || block instanceof BlockFenceGate;
    }

    @Override
    public boolean skyExposed(int x, int y, int z) {
        if (!skylight) {
            return false;
        }
        cursor.setPos(x, y, z);
        // The rain height counts anything that blocks movement or liquid, so glass and leaves are cover.
        return world.getPrecipitationHeight(cursor).getY() <= y;
    }
}
