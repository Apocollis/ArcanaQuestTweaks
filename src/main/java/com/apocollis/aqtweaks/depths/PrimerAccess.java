package com.apocollis.aqtweaks.depths;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.ChunkPrimer;

/**
 * Direct primer/state access for the Depths carve path.
 *
 * <p>The Depths mixins are {@code remap = false}, so vanilla member names written inside those
 * classes are not remapped and have to go through {@link com.apocollis.aqtweaks.util.Reflect}.
 * Reflective invoke costs far too much for a per-block worldgen loop, so the vanilla calls live
 * here instead — this is one of Tweaks' own classes and is remapped at build time.
 *
 * <p>Null and throw behaviour matches {@code Reflect} so carve results are unchanged.
 */
public final class PrimerAccess {

    private static IBlockState airState;

    private PrimerAccess() {}

    private static IBlockState air() {
        IBlockState state = airState;
        if (state == null) {
            state = Blocks.AIR.getDefaultState();
            airState = state;
        }
        return state;
    }

    public static IBlockState getBlockState(ChunkPrimer primer, int x, int y, int z) {
        if (primer == null) return air();
        try {
            IBlockState state = primer.getBlockState(x, y, z);
            return state != null ? state : air();
        } catch (Throwable t) {
            return air();
        }
    }

    public static void setBlockState(ChunkPrimer primer, int x, int y, int z, IBlockState state) {
        if (primer == null || state == null) return;
        try {
            primer.setBlockState(x, y, z, state);
        } catch (Throwable t) {}
    }

    public static Block getBlock(IBlockState state) {
        if (state == null) return Blocks.AIR;
        try {
            Block block = state.getBlock();
            return block != null ? block : Blocks.AIR;
        } catch (Throwable t) {
            return Blocks.AIR;
        }
    }

    public static Material getMaterial(IBlockState state) {
        if (state == null) return Material.AIR;
        try {
            Material material = state.getMaterial();
            return material != null ? material : Material.AIR;
        } catch (Throwable t) {
            return Material.AIR;
        }
    }

    /**
     * Highest Y that is neither air nor water with only air/water above it, or 64 when there is none.
     *
     * <p>Scanning down from the top, the first non-air/non-water block <em>is</em> that surface —
     * everything above it was already open — so one pass is enough. A solid block at Y 255 keeps the
     * old fallback of 64 rather than reporting the build limit as ground.
     */
    public static int openSkySurfaceY(ChunkPrimer primer, int x, int z) {
        if (primer == null) return 64;
        if (isOpaque(primer, x, 255, z)) return 64;
        for (int y = 254; y >= 1; --y) {
            if (isOpaque(primer, x, y, z)) return y;
        }
        return 64;
    }

    private static boolean isOpaque(ChunkPrimer primer, int x, int y, int z) {
        IBlockState state = getBlockState(primer, x, y, z);
        Material material = getMaterial(state);
        return getBlock(state) != Blocks.AIR && material != Material.AIR && material != Material.WATER;
    }
}
