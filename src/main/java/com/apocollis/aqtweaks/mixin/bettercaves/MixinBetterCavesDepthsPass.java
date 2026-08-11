package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;
import com.yungnickyoung.minecraft.bettercaves.world.MapGenBetterCaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import net.minecraftforge.common.BiomeDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After native Better Caves: carve breach tunnels Y4→-25 through Y0 (does not cancel BC).
 * At the Y0 seam, expands mouths by 1 block radius for walkable connections.
 */
@Mixin(value = MapGenBetterCaves.class, remap = false)
public abstract class MixinBetterCavesDepthsPass {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-BreachPass");
    private static final float BREACH_THRESHOLD = 0.075f;

    private static FastNoise breachNoise1;
    private static FastNoise breachNoise2;
    private static boolean noiseInitialized = false;
    private static boolean loggedOnce = false;

    private static synchronized void initNoise(long seed) {
        if (!noiseInitialized) {
            int seed1 = (int) (seed & 0xFFFF);
            int seed2 = (int) ((seed >> 16) & 0xFFFF);

            breachNoise1 = new FastNoise(seed1 + 1111);
            breachNoise1.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            breachNoise1.SetFrequency(0.018f);
            breachNoise1.SetFractalOctaves(1);
            breachNoise1.SetFractalGain(0.3f);

            breachNoise2 = new FastNoise(seed2 + 2222);
            breachNoise2.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            breachNoise2.SetFrequency(0.018f);
            breachNoise2.SetFractalOctaves(1);
            breachNoise2.SetFractalGain(0.3f);

            noiseInitialized = true;
        }
    }

    private static boolean isWaterBiome(World world, int x, int z) {
        if (world == null) return false;
        try {
            Biome biome = null;
            if (world.getBiomeProvider() != null) {
                biome = world.getBiomeProvider().getBiome(new BlockPos(x, 64, z), Biomes.PLAINS);
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
                return name.contains("ocean") || name.contains("river") || name.contains("beach")
                        || name.contains("coral") || name.contains("kelp");
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private static void tryCarve(ChunkPrimer primer, int localX, int localZ, int y,
                                 IBlockState airState, net.minecraft.block.Block airBlock,
                                 net.minecraft.block.Block bedrockBlock) {
        if (localX < 0 || localX > 15 || localZ < 0 || localZ > 15) return;
        IBlockState cur = Reflect.getBlockState(primer, localX, y, localZ);
        net.minecraft.block.Block b = Reflect.getBlock(cur);
        if (cur == null || (airBlock != null && b == airBlock) || (bedrockBlock != null && b == bedrockBlock)) {
            return;
        }
        Reflect.setBlockState(primer, localX, y, localZ, airState);
    }

    @Inject(method = "func_186125_a", at = @At("RETURN"))
    private void onAfterBetterCavesBreachTunnels(World worldIn, int chunkX, int chunkZ, ChunkPrimer primer, CallbackInfo ci) {
        if (!ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule
                || !ArcanaQuestTweaksConfig.depthsModule.enableBetterCavesNegativeY) {
            return;
        }
        if (worldIn == null || primer == null) return;
        if (worldIn.provider != null && worldIn.provider.getDimension() != 0) return;

        int minY = ArcanaQuestTweaksConfig.depthsModule.minWorldY;
        if (minY >= 0) return;

        initNoise(Reflect.getSeed(worldIn));

        if (!loggedOnce) {
            LOGGER.info("[AQ-DEPTHS] BC companion breach pass (Y4→-25, widened Y0 mouths).");
            loggedOnce = true;
        }

        IBlockState airState = Reflect.getAirState();
        net.minecraft.block.Block airBlock = Reflect.getAirBlock();
        net.minecraft.block.Block bedrockBlock = Reflect.getBedrockBlock();

        int breachTop = 4;
        int breachBottom = -25;
        int height = breachTop - breachBottom + 1;
        int startX = chunkX * 16;
        int startZ = chunkZ * 16;
        float xz = 1.0f;
        float yComp = 0.70f;

        // First pass: mark cores, second would need buffer — carve core then expand seam
        boolean[][] seamCore = new boolean[16][16];

        for (int localX = 0; localX < 16; ++localX) {
            int worldX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int worldZ = startZ + localZ;
                if (isWaterBiome(worldIn, worldX, worldZ)) continue;

                float[] v1 = new float[height];
                float[] v2 = new float[height];
                for (int y = breachBottom; y <= breachTop; y += 4) {
                    int idx = y - breachBottom;
                    v1[idx] = breachNoise1.GetNoise(worldX * xz, y * yComp, worldZ * xz);
                    v2[idx] = breachNoise2.GetNoise(worldX * xz, y * yComp, worldZ * xz);
                }
                int last = height - 1;
                if (last % 4 != 0) {
                    v1[last] = breachNoise1.GetNoise(worldX * xz, breachTop * yComp, worldZ * xz);
                    v2[last] = breachNoise2.GetNoise(worldX * xz, breachTop * yComp, worldZ * xz);
                }
                for (int sub = 0; sub < height - 1; sub += 4) {
                    int end = Math.min(sub + 4, height - 1);
                    float s1 = v1[sub], e1 = v1[end];
                    float s2 = v2[sub], e2 = v2[end];
                    int span = end - sub;
                    for (int i = 1; i < span; ++i) {
                        float t = (float) i / (float) span;
                        v1[sub + i] = s1 * (1.0f - t) + e1 * t;
                        v2[sub + i] = s2 * (1.0f - t) + e2 * t;
                    }
                }
                for (int idx = height - 1; idx >= 0; --idx) {
                    if (v1[idx] >= BREACH_THRESHOLD && v2[idx] >= BREACH_THRESHOLD) {
                        if (idx - 1 >= 0) {
                            v1[idx - 1] = 0.10f * v1[idx - 1] + 0.90f * v1[idx];
                            v2[idx - 1] = 0.10f * v2[idx - 1] + 0.90f * v2[idx];
                        }
                        if (idx - 2 >= 0) {
                            v1[idx - 2] = 0.30f * v1[idx - 2] + 0.70f * v1[idx];
                            v2[idx - 2] = 0.30f * v2[idx - 2] + 0.70f * v2[idx];
                        }
                        if (idx - 3 >= 0) {
                            v1[idx - 3] = 0.50f * v1[idx - 3] + 0.50f * v1[idx];
                            v2[idx - 3] = 0.50f * v2[idx - 3] + 0.50f * v2[idx];
                        }
                    }
                }

                for (int y = breachBottom; y <= breachTop; ++y) {
                    if (y < minY + 4) continue;
                    int idx = y - breachBottom;
                    if (v1[idx] > BREACH_THRESHOLD && v2[idx] > BREACH_THRESHOLD) {
                        tryCarve(primer, localX, localZ, y, airState, airBlock, bedrockBlock);
                        if (y >= -2 && y <= 2) {
                            seamCore[localX][localZ] = true;
                        }
                    }
                }
            }
        }

        // Widen Y0 seam mouths (3×3) so +Y / -Y connect walkably
        for (int localX = 0; localX < 16; ++localX) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                if (!seamCore[localX][localZ]) continue;
                for (int y = -2; y <= 2; ++y) {
                    for (int dx = -1; dx <= 1; ++dx) {
                        for (int dz = -1; dz <= 1; ++dz) {
                            tryCarve(primer, localX + dx, localZ + dz, y, airState, airBlock, bedrockBlock);
                        }
                    }
                }
            }
        }
    }
}
