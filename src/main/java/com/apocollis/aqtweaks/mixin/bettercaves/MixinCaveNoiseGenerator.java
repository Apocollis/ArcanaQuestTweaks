package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
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
 * Depths Update CaveNoiseGenerator replace: aligns primer carve with universal layer map.
 * Does not carve Y>=-1 (seam owned by ChunkProviderServer universal pass + breach mouths).
 */
@Mixin(targets = "sayys.depthsupdate.world.generation.noise.CaveNoiseGenerator", remap = false)
public abstract class MixinCaveNoiseGenerator {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-DepthsBetterCaves");

    private static final float CAVE_THRESHOLD = 0.28f;
    private static final float MID_CAVE_THRESHOLD = 0.32f;
    private static final float CAVERN_THRESHOLD = 0.045f;
    private static final int MAX_PRIMER_CARVE_Y = -2;

    private static FastNoise tunnelNoise1;
    private static FastNoise tunnelNoise2;
    private static FastNoise cavernNoise1;
    private static FastNoise cavernNoise2;
    private static FastNoise midCaveNoise1;
    private static FastNoise midCaveNoise2;
    private static FastNoise floorIslandNoise;

    private static boolean noiseInitialized = false;
    private static boolean loggedOnce = false;

    private World capturedWorld;

    @Inject(method = "<init>(Lnet/minecraft/world/World;)V", remap = false, at = @At("RETURN"))
    private void onInitDepthsBetterCaves(World world, CallbackInfo ci) {
        this.capturedWorld = world;
        initNoiseIfNeeded(world != null ? Reflect.getSeed(world) : 1337L);
    }

    private static synchronized void initNoiseIfNeeded(long worldSeed) {
        if (!noiseInitialized) {
            int seed1 = (int) (worldSeed & 0xFFFF);
            int seed2 = (int) ((worldSeed >> 16) & 0xFFFF);

            tunnelNoise1 = new FastNoise(seed1 + 1111);
            tunnelNoise1.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            tunnelNoise1.SetFrequency(0.025f);
            tunnelNoise1.SetFractalOctaves(1);
            tunnelNoise1.SetFractalGain(0.3f);

            tunnelNoise2 = new FastNoise(seed2 + 2222);
            tunnelNoise2.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            tunnelNoise2.SetFrequency(0.025f);
            tunnelNoise2.SetFractalOctaves(1);
            tunnelNoise2.SetFractalGain(0.3f);

            cavernNoise1 = new FastNoise(seed1 + 3333);
            cavernNoise1.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            cavernNoise1.SetFrequency(0.015f);
            cavernNoise1.SetFractalOctaves(1);
            cavernNoise1.SetFractalGain(0.3f);

            cavernNoise2 = new FastNoise(seed2 + 4444);
            cavernNoise2.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            cavernNoise2.SetFrequency(0.015f);
            cavernNoise2.SetFractalOctaves(1);
            cavernNoise2.SetFractalGain(0.3f);

            midCaveNoise1 = new FastNoise(seed1 + 5555);
            midCaveNoise1.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            midCaveNoise1.SetFrequency(0.040f);
            midCaveNoise1.SetFractalOctaves(1);
            midCaveNoise1.SetFractalGain(0.3f);

            midCaveNoise2 = new FastNoise(seed2 + 6666);
            midCaveNoise2.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            midCaveNoise2.SetFrequency(0.040f);
            midCaveNoise2.SetFractalOctaves(1);
            midCaveNoise2.SetFractalGain(0.3f);

            floorIslandNoise = new FastNoise(seed2 + 9876);
            floorIslandNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            floorIslandNoise.SetFrequency(0.035f);

            noiseInitialized = true;
        }
    }

    private static boolean isWaterBiome(World world, int x, int z) {
        if (world == null) return false;
        try {
            Biome biome = null;
            Biome fallbackBiome = Reflect.getPlainsBiome();
            if (world.getBiomeProvider() != null) {
                biome = world.getBiomeProvider().getBiome(new BlockPos(x, 64, z), fallbackBiome);
            }
            if (biome == null) return false;

            if (BiomeDictionary.hasType(biome, BiomeDictionary.Type.WATER) ||
                BiomeDictionary.hasType(biome, BiomeDictionary.Type.OCEAN) ||
                BiomeDictionary.hasType(biome, BiomeDictionary.Type.RIVER) ||
                BiomeDictionary.hasType(biome, BiomeDictionary.Type.BEACH)) {
                return true;
            }

            if (biome.getRegistryName() != null) {
                String name = biome.getRegistryName().toString().toLowerCase();
                return name.contains("ocean") || name.contains("deep_ocean") ||
                       name.contains("beach") || name.contains("river") ||
                       name.contains("coral") || name.contains("kelp");
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Inject(method = "generate(IILnet/minecraft/world/chunk/ChunkPrimer;[Lnet/minecraft/world/biome/Biome;)V", remap = false, at = @At("HEAD"), cancellable = true)
    private void onGenerateDepthsBetterCaves(int chunkX, int chunkZ, ChunkPrimer primer, Biome[] biomes, CallbackInfo ci) {
        if (ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule && ArcanaQuestTweaksConfig.depthsModule.enableBetterCavesNegativeY) {
            if (primer == null) return;

            int minY = ArcanaQuestTweaksConfig.depthsModule.minWorldY;
            if (minY >= 0) return;

            long seed = this.capturedWorld != null ? Reflect.getSeed(this.capturedWorld) : 1337L;
            initNoiseIfNeeded(seed);

            if (!loggedOnce) {
                LOGGER.info("[AQ-DEPTHS] Primer -Y carve aligned: caverns→{}, mid→{}, breach roots→{}, maxY={}",
                        ArcanaQuestTweaksConfig.depthsModule.cavernTopY,
                        ArcanaQuestTweaksConfig.depthsModule.midCaveTopY,
                        ArcanaQuestTweaksConfig.depthsModule.caveBottomY,
                        MAX_PRIMER_CARVE_Y);
                loggedOnce = true;
            }

            int caveTop = Math.min(MAX_PRIMER_CARVE_Y, ArcanaQuestTweaksConfig.depthsModule.caveTopY);
            int caveBottom = ArcanaQuestTweaksConfig.depthsModule.caveBottomY;
            float caveXzComp = ArcanaQuestTweaksConfig.depthsModule.caveXzCompression;
            float caveYComp = ArcanaQuestTweaksConfig.depthsModule.caveYCompression;

            int cavernTop = ArcanaQuestTweaksConfig.depthsModule.cavernTopY;
            int cavernBottom = Math.max(minY + 4, ArcanaQuestTweaksConfig.depthsModule.cavernBottomY);
            float cavernXzComp = ArcanaQuestTweaksConfig.depthsModule.cavernXzCompression;
            float cavernYComp = ArcanaQuestTweaksConfig.depthsModule.cavernYCompression;

            int midTop = Math.min(MAX_PRIMER_CARVE_Y, ArcanaQuestTweaksConfig.depthsModule.midCaveTopY);
            int midBottom = ArcanaQuestTweaksConfig.depthsModule.midCaveBottomY;

            boolean oceanFlooding = ArcanaQuestTweaksConfig.depthsModule.enableOceanWaterCaves;

            IBlockState bedrockState = Reflect.getBedrockState();
            IBlockState airState = Reflect.getAirState();
            IBlockState lavaState = Reflect.getLavaState();
            IBlockState waterState = Reflect.getWaterState();
            net.minecraft.block.Block airBlock = Reflect.getAirBlock();
            net.minecraft.block.Block bedrockBlock = Reflect.getBedrockBlock();

            int startX = chunkX * 16;
            int startZ = chunkZ * 16;
            int lavaLevel = minY + 9;

            int caveHeight = Math.max(1, caveTop - caveBottom + 1);
            int cavernHeight = Math.max(1, cavernTop - cavernBottom + 1);
            int midHeight = Math.max(1, midTop - midBottom + 1);

            int cavernCeilingStart = cavernTop - 8;
            int cavernFloorEnd = cavernBottom < lavaLevel ? lavaLevel + 8 : cavernBottom + 7;

            int loopMaxY = Math.min(MAX_PRIMER_CARVE_Y, Math.max(caveTop, Math.max(cavernTop, midTop)));

            for (int localX = 0; localX < 16; ++localX) {
                int worldX = startX + localX;
                for (int localZ = 0; localZ < 16; ++localZ) {
                    int worldZ = startZ + localZ;

                    for (int y = minY; y <= minY + 3; ++y) {
                        Reflect.setBlockState(primer, localX, y, localZ, bedrockState);
                    }

                    boolean isWater = oceanFlooding && isWaterBiome(this.capturedWorld, worldX, worldZ);
                    float floorVal = floorIslandNoise.GetNoise(worldX, worldZ);

                    float[] tunnelVal1 = new float[caveHeight];
                    float[] tunnelVal2 = new float[caveHeight];
                    sampleDual(tunnelNoise1, tunnelNoise2, worldX, worldZ, caveBottom, caveTop, caveXzComp, caveYComp, tunnelVal1, tunnelVal2);

                    float[] cavernVal1 = new float[cavernHeight];
                    float[] cavernVal2 = new float[cavernHeight];
                    sampleDual(cavernNoise1, cavernNoise2, worldX, worldZ, cavernBottom, cavernTop, 1.0f, 1.0f, cavernVal1, cavernVal2);

                    float[] midVal1 = new float[midHeight];
                    float[] midVal2 = new float[midHeight];
                    sampleDual(midCaveNoise1, midCaveNoise2, worldX, worldZ, midBottom, midTop, 1.0f, 1.4f, midVal1, midVal2);

                    for (int y = minY + 4; y <= loopMaxY; ++y) {
                        IBlockState currentState = Reflect.getBlockState(primer, localX, y, localZ);
                        net.minecraft.block.Block currentBlock = Reflect.getBlock(currentState);
                        if (currentState == null || (airBlock != null && currentBlock == airBlock) || (bedrockBlock != null && currentBlock == bedrockBlock)) {
                            continue;
                        }

                        boolean carve = false;

                        // Breach tunnel roots (below seam)
                        if (y >= caveBottom && y <= caveTop) {
                            int idx = y - caveBottom;
                            if (idx >= 0 && idx < caveHeight
                                    && tunnelVal1[idx] >= CAVE_THRESHOLD
                                    && tunnelVal2[idx] >= CAVE_THRESHOLD) {
                                carve = true;
                            }
                        }

                        // Deep caverns
                        if (!carve && y >= cavernBottom && y <= cavernTop) {
                            int idx = y - cavernBottom;
                            if (idx >= 0 && idx < cavernHeight) {
                                float product = cavernVal1[idx] * cavernVal2[idx];
                                float currentThreshold = CAVERN_THRESHOLD;
                                if (y >= cavernCeilingStart) {
                                    float frac = (float) (cavernTop - y) / (float) Math.max(1, cavernTop - cavernCeilingStart);
                                    currentThreshold *= MathHelper.clamp(frac, 0.0f, 1.0f);
                                }
                                if (y < cavernFloorEnd) {
                                    float frac = (float) (y - cavernBottom) / (float) Math.max(1, cavernFloorEnd - cavernBottom);
                                    currentThreshold *= MathHelper.clamp(frac, 0.0f, 1.0f);
                                }
                                if (product < currentThreshold) {
                                    carve = true;
                                }
                            }
                        }

                        // Mid chambers
                        if (!carve && y >= midBottom && y <= midTop) {
                            int idx = y - midBottom;
                            if (idx >= 0 && idx < midHeight) {
                                float product = midVal1[idx] * midVal2[idx];
                                if (product > MID_CAVE_THRESHOLD) {
                                    carve = true;
                                }
                            }
                        }

                        if (carve) {
                            if (isWater) {
                                if (y <= lavaLevel && floorVal > 0.12f) {
                                    continue;
                                }
                                Reflect.setBlockState(primer, localX, y, localZ, waterState);
                            } else if (y <= lavaLevel) {
                                if (floorVal > 0.12f) {
                                    continue;
                                }
                                Reflect.setBlockState(primer, localX, y, localZ, lavaState);
                            } else {
                                Reflect.setBlockState(primer, localX, y, localZ, airState);
                            }
                        }
                    }
                }
            }

            ci.cancel();
        }
    }

    private static void sampleDual(FastNoise n1, FastNoise n2, int worldX, int worldZ,
                                   int yBottom, int yTop, float xzComp, float yComp,
                                   float[] out1, float[] out2) {
        int height = yTop - yBottom + 1;
        if (height <= 0) return;

        for (int y = yBottom; y <= yTop; y += 4) {
            int idx = y - yBottom;
            if (idx >= 0 && idx < height) {
                float tx = worldX * xzComp;
                float ty = y * yComp;
                float tz = worldZ * xzComp;
                out1[idx] = n1.GetNoise(tx, ty, tz);
                out2[idx] = n2.GetNoise(tx, ty, tz);
            }
        }
        int lastIdx = height - 1;
        if (lastIdx % 4 != 0) {
            float tx = worldX * xzComp;
            float ty = yTop * yComp;
            float tz = worldZ * xzComp;
            out1[lastIdx] = n1.GetNoise(tx, ty, tz);
            out2[lastIdx] = n2.GetNoise(tx, ty, tz);
        }
        for (int sub = 0; sub < height - 1; sub += 4) {
            int endIdx = Math.min(sub + 4, height - 1);
            float s1 = out1[sub], e1 = out1[endIdx];
            float s2 = out2[sub], e2 = out2[endIdx];
            int span = endIdx - sub;
            for (int i = 1; i < span; ++i) {
                float t = (float) i / (float) span;
                out1[sub + i] = s1 * (1.0f - t) + e1 * t;
                out2[sub + i] = s2 * (1.0f - t) + e2 * t;
            }
        }
    }
}
