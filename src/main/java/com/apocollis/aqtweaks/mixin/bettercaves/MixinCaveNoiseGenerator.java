package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
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

@Mixin(targets = "sayys.depthsupdate.world.generation.noise.CaveNoiseGenerator", remap = false)
public abstract class MixinCaveNoiseGenerator {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-DepthsBetterCaves");

    // Tunnel thresholds (dual-generator intersection: ALL >= threshold)
    private static final float BREACH_THRESHOLD = 0.30f;  // Dense winding tunnels (Y = 0 to -8)
    private static final float MID_THRESHOLD    = 0.28f;  // Frequent mid-depth caves (Y = -8 to -35)

    // Cavern threshold (signed product-multiplication: raw product < threshold)
    // YUNG's CavernCarver uses raw noise in [-1,1], product in [-1,1], carves when product < threshold
    private static final float CAVERN_THRESHOLD = 0.045f;

    // Small cave threshold (spaghetti-style: both |noise| < threshold)
    private static final float SMALL_CAVE_THRESHOLD = 0.24f;

    // Layer boundaries (Strictly capped at BREACH_TOP = 0)
    private static final int BREACH_TOP    =   0;
    private static final int BREACH_BOTTOM =  -8;

    // Transition blend zones
    private static final int BREACH_MID_BLEND_TOP = -6;
    private static final int BREACH_MID_BLEND_BOT = -10;

    // Yung's Dual Simplex Generators for Cave Tunnels
    private static FastNoise tunnelNoise1;
    private static FastNoise tunnelNoise2;

    // Yung's Dual Simplex Generators for Cavern Chambers (product-multiplication)
    private static FastNoise cavernNoise1;
    private static FastNoise cavernNoise2;

    // 2D Floor Island Generator for Deepslate Walkways/Bridges
    private static FastNoise floorIslandNoise;

    // Small cave segment generators (spaghetti-style)
    private static FastNoise smallCaveNoise1;
    private static FastNoise smallCaveNoise2;

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

            // Cave Tunnels: Yung's native SimplexFractal dual generators
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

            // Cavern Chambers: Yung's native dual generators for product-multiplication
            // Low frequency (0.015f) creates wide, smooth 3D spatial features = massive domed chambers
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

            // Small Cave Segments (spaghetti-style)
            smallCaveNoise1 = new FastNoise(seed1 + 5555);
            smallCaveNoise1.SetNoiseType(FastNoise.NoiseType.Simplex);
            smallCaveNoise1.SetFrequency(0.016f);

            smallCaveNoise2 = new FastNoise(seed2 + 6666);
            smallCaveNoise2.SetNoiseType(FastNoise.NoiseType.Simplex);
            smallCaveNoise2.SetFrequency(0.016f);

            // 2D Floor Island Noise for deep cavern platforms/bridges
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

    private static float getThresholdForY(int y) {
        return MID_THRESHOLD;
    }

    /**
     * Pre-carves negative Y terrain strictly for Y <= 0 (zero modification above Y = 0):
     * Method signature matching bytecode: (int, int, ChunkPrimer, Biome[])
     */
    @Inject(method = "generate(IILnet/minecraft/world/chunk/ChunkPrimer;[Lnet/minecraft/world/biome/Biome;)V", remap = false, at = @At("HEAD"), cancellable = true)
    private void onGenerateDepthsBetterCaves(int chunkX, int chunkZ, ChunkPrimer primer, Biome[] biomes, CallbackInfo ci) {
        if (ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule && ArcanaQuestTweaksConfig.depthsModule.enableBetterCavesNegativeY) {
            if (primer == null) return;

            int minY = ArcanaQuestTweaksConfig.depthsModule.minWorldY;
            if (minY >= 0) return;

            long seed = this.capturedWorld != null ? Reflect.getSeed(this.capturedWorld) : 1337L;
            initNoiseIfNeeded(seed);

            if (!loggedOnce) {
                LOGGER.info("[AQ-DEPTHS] Pre-carving native Better Caves -Y terrain (STRICTLY Y <= 0): Tunnels + Product-Multiplication Caverns + Small Caves.");
                loggedOnce = true;
            }

            // Config Settings - Strictly cap caveTop at Y=4 (breach cap) and caverns at Y=-12
            int caveTop = Math.min(4, ArcanaQuestTweaksConfig.depthsModule.caveTopY); // max 4
            int caveBottom = ArcanaQuestTweaksConfig.depthsModule.caveBottomY; // -60
            float caveXzComp = ArcanaQuestTweaksConfig.depthsModule.caveXzCompression; // 0.9f
            float caveYComp = ArcanaQuestTweaksConfig.depthsModule.caveYCompression;   // 2.2f

            int cavernTop = Math.min(-12, ArcanaQuestTweaksConfig.depthsModule.cavernTopY); // max -12
            int cavernBottom = Math.max(minY + 4, ArcanaQuestTweaksConfig.depthsModule.cavernBottomY); // -60
            float cavernXzComp = ArcanaQuestTweaksConfig.depthsModule.cavernXzCompression; // 0.7f
            float cavernYComp = ArcanaQuestTweaksConfig.depthsModule.cavernYCompression;   // 1.3f

            boolean oceanFlooding = ArcanaQuestTweaksConfig.depthsModule.enableOceanWaterCaves;

            IBlockState bedrockState = Reflect.getBedrockState();
            IBlockState airState = Reflect.getAirState();
            IBlockState lavaState = Reflect.getLavaState();
            IBlockState waterState = Reflect.getWaterState();
            net.minecraft.block.Block airBlock = Reflect.getAirBlock();
            net.minecraft.block.Block bedrockBlock = Reflect.getBedrockBlock();

            int startX = chunkX * 16;
            int startZ = chunkZ * 16;
            int lavaLevel = minY + 9; // Y = -55

            int caveHeight = caveTop - caveBottom + 1;
            int cavernHeight = cavernTop - cavernBottom + 1;

            int cavernCeilingStart = cavernTop - 6;
            int cavernFloorEnd;
            if (cavernBottom < lavaLevel) {
                cavernFloorEnd = lavaLevel + 8;
            } else {
                cavernFloorEnd = cavernBottom + 7;
            }

            for (int localX = 0; localX < 16; ++localX) {
                int worldX = startX + localX;
                for (int localZ = 0; localZ < 16; ++localZ) {
                    int worldZ = startZ + localZ;

                    // 0. Solid flat Bedrock 4 layers thick (Y=-64 to -61)
                    for (int y = minY; y <= minY + 3; ++y) {
                        Reflect.setBlockState(primer, localX, y, localZ, bedrockState);
                    }

                    boolean isWater = oceanFlooding && isWaterBiome(this.capturedWorld, worldX, worldZ);
                    float floorVal = floorIslandNoise.GetNoise(worldX, worldZ);

                    // 1. Sample Tunnel Noise
                    float[] tunnelVal1 = new float[caveHeight];
                    float[] tunnelVal2 = new float[caveHeight];

                    for (int y = caveBottom; y <= caveTop; y += 4) {
                        int idx = y - caveBottom;
                        if (idx >= 0 && idx < caveHeight) {
                            float tx = worldX * caveXzComp;
                            float ty = y * caveYComp;
                            float tz = worldZ * caveXzComp;
                            tunnelVal1[idx] = tunnelNoise1.GetNoise(tx, ty, tz);
                            tunnelVal2[idx] = tunnelNoise2.GetNoise(tx, ty, tz);
                        }
                    }
                    {
                        int lastIdx = caveHeight - 1;
                        if (lastIdx % 4 != 0) {
                            float tx = worldX * caveXzComp;
                            float ty = caveTop * caveYComp;
                            float tz = worldZ * caveXzComp;
                            tunnelVal1[lastIdx] = tunnelNoise1.GetNoise(tx, ty, tz);
                            tunnelVal2[lastIdx] = tunnelNoise2.GetNoise(tx, ty, tz);
                        }
                    }

                    for (int sub = 0; sub < caveHeight - 1; sub += 4) {
                        int endIdx = Math.min(sub + 4, caveHeight - 1);
                        float s1 = tunnelVal1[sub], e1 = tunnelVal1[endIdx];
                        float s2 = tunnelVal2[sub], e2 = tunnelVal2[endIdx];
                        int span = endIdx - sub;
                        for (int i = 1; i < span; ++i) {
                            float t = (float) i / (float) span;
                            tunnelVal1[sub + i] = s1 * (1.0f - t) + e1 * t;
                            tunnelVal2[sub + i] = s2 * (1.0f - t) + e2 * t;
                        }
                    }

                    // 2. Top-down yAdjust for smooth vaulted ceilings
                    for (int y = caveTop; y >= caveBottom; --y) {
                        int idx = y - caveBottom;
                        float threshold = getThresholdForY(y);

                        if (tunnelVal1[idx] >= threshold && tunnelVal2[idx] >= threshold) {
                            if (idx + 1 < caveHeight) {
                                tunnelVal1[idx + 1] = 0.05f * tunnelVal1[idx + 1] + 0.95f * tunnelVal1[idx];
                                tunnelVal2[idx + 1] = 0.05f * tunnelVal2[idx + 1] + 0.95f * tunnelVal2[idx];
                            }
                            if (idx + 2 < caveHeight) {
                                tunnelVal1[idx + 2] = 0.50f * tunnelVal1[idx + 2] + 0.50f * tunnelVal1[idx];
                                tunnelVal2[idx + 2] = 0.50f * tunnelVal2[idx + 2] + 0.50f * tunnelVal2[idx];
                            }
                        }
                    }

                    // 3. Sample Cavern Noise — Product Multiplication
                    float[] cavernVal1 = new float[cavernHeight];
                    float[] cavernVal2 = new float[cavernHeight];

                    for (int y = cavernBottom; y <= cavernTop; y += 4) {
                        int idx = y - cavernBottom;
                        if (idx >= 0 && idx < cavernHeight) {
                            // Sample at raw world coordinates — frequency is baked into FastNoise (0.015f)
                            // No compression multipliers: produces wide, round 3D chambers
                            cavernVal1[idx] = cavernNoise1.GetNoise(worldX, y, worldZ);
                            cavernVal2[idx] = cavernNoise2.GetNoise(worldX, y, worldZ);
                        }
                    }
                    {
                        int lastIdx = cavernHeight - 1;
                        if (lastIdx % 4 != 0) {
                            cavernVal1[lastIdx] = cavernNoise1.GetNoise(worldX, cavernTop, worldZ);
                            cavernVal2[lastIdx] = cavernNoise2.GetNoise(worldX, cavernTop, worldZ);
                        }
                    }

                    for (int sub = 0; sub < cavernHeight - 1; sub += 4) {
                        int endIdx = Math.min(sub + 4, cavernHeight - 1);
                        float s1 = cavernVal1[sub], e1 = cavernVal1[endIdx];
                        float s2 = cavernVal2[sub], e2 = cavernVal2[endIdx];
                        int span = endIdx - sub;
                        for (int i = 1; i < span; ++i) {
                            float t = (float) i / (float) span;
                            cavernVal1[sub + i] = s1 * (1.0f - t) + e1 * t;
                            cavernVal2[sub + i] = s2 * (1.0f - t) + e2 * t;
                        }
                    }

                    // 4. Main Carving Loop (Y = minY+4 up to caveTop)
                    for (int y = minY + 4; y <= caveTop; ++y) {
                        IBlockState currentState = Reflect.getBlockState(primer, localX, y, localZ);
                        net.minecraft.block.Block currentBlock = Reflect.getBlock(currentState);
                        if (currentState == null || (airBlock != null && currentBlock == airBlock) || (bedrockBlock != null && currentBlock == bedrockBlock)) {
                            continue;
                        }

                        boolean carve = false;

                        // Y=0 ceiling taper factor — smoothly reduces all carving to 0 as y approaches 0
                        float ceilingTaper = 1.0f;
                        if (y >= -4) {
                            ceilingTaper = MathHelper.clamp((float)(-y) / 4.0f, 0.0f, 1.0f);
                            if (ceilingTaper <= 0.0f) continue; // At Y=0+, never carve
                        }

                        // A. Tunnel Layers
                        if (y >= caveBottom && y <= caveTop) {
                            int idx = y - caveBottom;
                            float threshold = getThresholdForY(y);
                            // Ceiling taper raises the threshold, reducing carving near Y=0
                            float tapered = 1.0f - ((1.0f - threshold) * ceilingTaper);
                            if (tunnelVal1[idx] >= tapered && tunnelVal2[idx] >= tapered) {
                                carve = true;
                            }
                        }

                        // B. Small Cave Segments
                        if (!carve && y >= caveBottom && y <= caveTop) {
                            float s1 = smallCaveNoise1.GetNoise(worldX * 0.8f, y * 0.8f, worldZ * 0.8f);
                            float s2 = smallCaveNoise2.GetNoise(worldX * 0.8f, y * 0.8f, worldZ * 0.8f);
                            if (Math.abs(s1) < SMALL_CAVE_THRESHOLD && Math.abs(s2) < SMALL_CAVE_THRESHOLD) {
                                carve = true;
                            }
                        }

                        // C. Cavern Chambers (Y = cavernBottom to cavernTop)
                        if (!carve && y >= cavernBottom && y <= cavernTop) {
                            int idx = y - cavernBottom;

                            // YUNG's raw signed product: n1, n2 in [-1, 1], product in [-1, 1]
                            // When n1 and n2 have opposite signs → product < 0 → always carves
                            // When both same sign and small → product small positive → may carve
                            // When both same sign and large → product >> threshold → solid wall
                            float product = cavernVal1[idx] * cavernVal2[idx];

                            float currentThreshold = CAVERN_THRESHOLD;

                            // Ceiling dome tapering
                            if (y >= cavernCeilingStart) {
                                float frac = (float) (cavernTop - y) / (float) Math.max(1, cavernTop - cavernCeilingStart);
                                currentThreshold *= MathHelper.clamp(frac, 0.0f, 1.0f);
                            }

                            // Floor basin tapering
                            if (y < cavernFloorEnd) {
                                float frac = (float) (y - cavernBottom) / (float) Math.max(1, cavernFloorEnd - cavernBottom);
                                currentThreshold *= MathHelper.clamp(frac, 0.0f, 1.0f);
                            }

                            // Apply Y=0 ceiling taper
                            currentThreshold *= ceilingTaper;

                            // YUNG's CavernCarver: carve when raw signed product < threshold
                            if (product < currentThreshold) {
                                carve = true;
                            }
                        }

                        // 5. Block Placement
                        if (carve) {
                            if (isWater) {
                                if (y <= lavaLevel && floorVal > 0.12f) {
                                    continue;
                                } else {
                                    Reflect.setBlockState(primer, localX, y, localZ, waterState);
                                }
                            } else {
                                if (y <= lavaLevel) {
                                    if (floorVal > 0.12f) {
                                        continue;
                                    } else {
                                        Reflect.setBlockState(primer, localX, y, localZ, lavaState);
                                    }
                                } else {
                                    Reflect.setBlockState(primer, localX, y, localZ, airState);
                                }
                            }
                        }
                    }
                }
            }

            ci.cancel();
        }
    }
}
