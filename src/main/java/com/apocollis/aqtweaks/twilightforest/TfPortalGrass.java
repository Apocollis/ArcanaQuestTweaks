package com.apocollis.aqtweaks.twilightforest;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.block.material.Material;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import twilightforest.world.TFWorld;

public final class TfPortalGrass {

    private TfPortalGrass() {
    }

    public static boolean isGrass(World world, BlockPos pos) {
        return world.getBlockState(pos).getMaterial() == Material.GRASS;
    }

    /**
     * Y of a {@link Material#GRASS} block in the column, scanning up from TF sea level.
     * {@code Integer.MIN_VALUE} if none (dirt/stone/canopy-only columns are rejected).
     */
    public static int findGrassBlockY(World world, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = TFWorld.SEALEVEL; y < TFWorld.CHUNKHEIGHT - 1; y++) {
            pos.setPos(x, y, z);
            if (isGrass(world, pos)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    public static boolean hasGrassColumn(World world, int x, int z) {
        return findGrassBlockY(world, x, z) != Integer.MIN_VALUE;
    }

    public static boolean platformIsGrass(World world, int originX, int platformY, int originZ,
            int platformWidth, int platformLength, boolean verticalZ) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int widthOffset = 0; widthOffset < platformWidth; widthOffset++) {
            for (int lengthOffset = 0; lengthOffset < platformLength; lengthOffset++) {
                int ox = verticalZ ? lengthOffset : widthOffset;
                int oz = verticalZ ? widthOffset : lengthOffset;
                if (!isGrass(world, pos.setPos(originX + ox, platformY, originZ + oz))) {
                    return false;
                }
            }
        }
        return true;
    }

    public static boolean biomeAllowed(Biome biome) {
        ResourceLocation id = biome.getRegistryName();
        String name = id == null ? "" : id.toString();
        var general = ArcanaQuestTweaksConfig.TwilightForestModuleConfig.general;
        if (hasEntries(general.safeBiomeAllowlist) && !containsIgnoreCase(general.safeBiomeAllowlist, name)) {
            return false;
        }
        return !containsIgnoreCase(general.alwaysUnsafeBiomes, name);
    }

    private static boolean hasEntries(String[] list) {
        if (list == null) {
            return false;
        }
        for (String s : list) {
            if (s != null && !s.trim().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private static boolean containsIgnoreCase(String[] list, String name) {
        if (list == null || name.isEmpty()) {
            return false;
        }
        for (String s : list) {
            if (s != null && name.equalsIgnoreCase(s.trim())) {
                return true;
            }
        }
        return false;
    }
}
