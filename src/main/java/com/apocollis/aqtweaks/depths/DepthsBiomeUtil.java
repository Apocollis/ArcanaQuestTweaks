package com.apocollis.aqtweaks.depths;

import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.BiomeDictionary;

/**
 * Shared biome checks for depths seam / water flood behavior.
 */
public final class DepthsBiomeUtil {

    private DepthsBiomeUtil() {}

    public static boolean isWaterBiome(World world, int x, int z) {
        if (world == null) return false;
        try {
            Biome biome = null;
            Biome fallback = Reflect.getPlainsBiome();
            if (world.getBiomeProvider() != null) {
                biome = world.getBiomeProvider().getBiome(new BlockPos(x, 64, z), fallback);
            }
            if (biome == null) return false;

            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.WATER)
                    || BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)
                    || BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER)
                    || BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH)) {
                return true;
            }

            if (biome.getRegistryName() != null) {
                String name = biome.getRegistryName().toString().toLowerCase();
                return name.contains("ocean") || name.contains("deep_ocean")
                        || name.contains("beach") || name.contains("river")
                        || name.contains("coral") || name.contains("kelp");
            }
        } catch (Throwable ignored) {
        }
        return false;
    }
}
