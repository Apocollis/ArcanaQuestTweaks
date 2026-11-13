package com.apocollis.aqtweaks.client.aurorian;

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * Cover height per column for the roof kill: the top of the highest blocking block that is not leaves, so a roof,
 * an overhang or a cave ceiling counts and a tree canopy does not (the hazard shelter rule ignores leaves too).
 * Memoised per second; computed once per column, so a particle costs one hash lookup.
 */
@SideOnly(Side.CLIENT)
public final class MoonSnowRoof {
    private MoonSnowRoof() {}

    private static final Long2IntOpenHashMap MEMO = new Long2IntOpenHashMap();
    private static long stamp = Long.MIN_VALUE;
    private static final BlockPos.MutableBlockPos POS = new BlockPos.MutableBlockPos();

    static {
        MEMO.defaultReturnValue(-1);
    }

    public static void reset() {
        MEMO.clear();
        stamp = Long.MIN_VALUE;
    }

    /** Cover height of the column, or -1 when its chunk is not loaded. */
    public static int cover(World world, int x, int z) {
        long second = world.getTotalWorldTime() / 20L;
        if (second != stamp) {
            MEMO.clear();
            stamp = second;
        }
        long key = ((long) x & 0xFFFFFFFFL) | ((long) z << 32);
        int cached = MEMO.get(key);
        if (cached >= 0) return cached;
        POS.setPos(x, 64, z);
        if (!world.isBlockLoaded(POS)) return -1;
        int top = world.getPrecipitationHeight(POS).getY();
        int cover = 0;
        for (int y = top - 1; y >= Math.max(0, top - 64); y--) {
            POS.setPos(x, y, z);
            IBlockState state = world.getBlockState(POS);
            if (state.getMaterial().blocksMovement() || state.getMaterial().isLiquid()) {
                if (state.getBlock().isLeaves(state, world, POS)) continue;
                cover = y + 1;
                break;
            }
        }
        MEMO.put(key, cover);
        return cover;
    }

    /** True when the block at the position is loaded and solid. */
    public static boolean solidAt(World world, double x, double y, double z) {
        POS.setPos(x, y, z);
        if (!world.isBlockLoaded(POS)) return false;
        return world.getBlockState(POS).getMaterial().isSolid();
    }
}
