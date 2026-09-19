package com.apocollis.aqtweaks.depths;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.ResourceLocation;

/**
 * Better Caves {@code canReplaceBlock} allow-list, with direct vanilla access.
 *
 * <p>{@code MixinCarverUtils} is {@code remap = false}, so this logic lives here. Tweaks only
 * forces {@code true} (carve through). A miss must fall through to parent Better Caves — never
 * force {@code false}.
 */
public final class BetterCavesReplaceable {

    private BetterCavesReplaceable() {}

    /**
     * @return {@code true} when Tweaks knows this block is terrain BC should carve;
     *         {@code false} when Tweaks does not decide (parent {@code canReplaceBlock} runs)
     */
    public static boolean allow(IBlockState state) {
        if (state == null) return false;
        Block block;
        Material mat;
        try {
            block = state.getBlock();
            mat = state.getMaterial();
        } catch (Throwable t) {
            return false;
        }
        if (block == null || mat == null) return false;
        if (mat == Material.AIR || mat == Material.WATER || mat == Material.LAVA) return false;

        if (mat == Material.ROCK
                || mat == Material.GROUND
                || mat == Material.CLAY
                || mat == Material.SAND
                || mat == Material.GRASS
                || mat == Material.ICE
                || mat == Material.PACKED_ICE
                || mat == Material.CRAFTED_SNOW) {
            return true;
        }

        String name;
        try {
            ResourceLocation id = block.getRegistryName();
            name = id == null ? "" : id.toString().toLowerCase();
        } catch (Throwable t) {
            return false;
        }
        return name.contains("stone")
                || name.contains("deepslate")
                || name.contains("clay")
                || name.contains("terracotta")
                || name.contains("dirt")
                || name.contains("sand")
                || name.contains("rock")
                || name.contains("granite")
                || name.contains("diorite")
                || name.contains("andesite")
                || name.contains("basalt")
                || name.contains("tuff")
                || name.contains("slate");
    }
}
