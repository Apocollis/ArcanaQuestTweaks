package com.apocollis.aqtweaks.bettermineshafts;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeProvider;
import net.minecraftforge.common.BiomeDictionary;

import java.util.Random;

/**
 * Same spawn decision for locate and chunk generate: chunk spacing first, then biome
 * <em>provider</em> at Y=64 (not loaded-chunk biome), then Tweaks rate. Layout still
 * consumes {@code MapGenBase.rand} once on land so {@code getStructureStart} stays on
 * the parent stream.
 */
public final class BetterMineshaftCanSpawn {
    private BetterMineshaftCanSpawn() {}

    /**
     * @return {@code null} if Tweaks placement is off or world/provider is missing
     *         (caller should use parent canSpawn)
     */
    public static Boolean test(Object mapGen, int chunkX, int chunkZ) {
        if (!ArcanaQuestTweaksConfig.BetterMineshaftsModuleConfig.general.enable) {
            return null;
        }
        World world = Reflect.getMapGenWorld(mapGen);
        if (world == null) {
            return null;
        }
        int spacing = Math.max(1, ArcanaQuestTweaksConfig.BetterMineshaftsModuleConfig.general.chunkSpacing);
        if (Math.floorMod(chunkX, spacing) != 0 || Math.floorMod(chunkZ, spacing) != 0) {
            return Boolean.FALSE;
        }
        if (ArcanaQuestTweaksConfig.BetterMineshaftsModuleConfig.general.skipOceanAndBeach) {
            BiomeProvider provider = Reflect.getBiomeProvider(world);
            int x = (chunkX << 4) + 8;
            int z = (chunkZ << 4) + 8;
            Biome biome = Reflect.getBiome(provider, x, z);
            if (biome != null && (BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN)
                    || BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH))) {
                return Boolean.FALSE;
            }
        }
        long seed = Reflect.getSeed(world);
        Random decision = new Random((chunkX * 341873128712L) ^ (chunkZ * 132897987541L) ^ seed);
        decision.nextInt();
        double rate = ArcanaQuestTweaksConfig.BetterMineshaftsModuleConfig.general.mineshaftSpawnRate;
        boolean spawn = decision.nextDouble() < Math.max(0.0, Math.min(1.0, rate));
        Random layout = Reflect.getMapGenRandom(mapGen);
        if (layout != null) {
            layout.nextDouble();
        }
        return spawn;
    }
}
