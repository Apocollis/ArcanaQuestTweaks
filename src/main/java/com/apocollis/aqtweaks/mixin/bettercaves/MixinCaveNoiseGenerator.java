package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.ChunkPrimer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Depths primer -Y authority (replaces Depths cave generate).
 * All -Y carve + decor happen here so structures stick (Chunk -Y writes are unreliable).
 */
@Mixin(targets = "sayys.depthsupdate.world.generation.noise.CaveNoiseGenerator", remap = false)
public abstract class MixinCaveNoiseGenerator {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-DepthsBetterCaves");

    private static final float BREACH_THRESHOLD = 0.075f;
    /** Abs dual-noise tube width → ~4–5 block diameter */
    private static final float UPPER_TUNNEL_WIDTH = 0.24f;
    private static final float UPPER_CHAMBER_THRESHOLD = 0.30f;
    private static final float LOWER_CAVERN_OPEN = 0.45f;

    private static final float COLUMN_RADIUS = 3.75f;
    private static final int COLUMN_SPACING = 24;

    /** Hard max Y for lower underworld carve (keeps upper band solid) */
    private static final int LOWER_MAX_Y = -26;
    private static final int MAX_PRIMER_CARVE_Y = -2;

    private static FastNoise breachNoise1;
    private static FastNoise breachNoise2;
    private static FastNoise upperChamber1;
    private static FastNoise upperChamber2;
    private static FastNoise upperTunnel1;
    private static FastNoise upperTunnel2;
    private static FastNoise lowerCavern1;
    private static FastNoise lowerCavern2;
    private static FastNoise floorIslandNoise;
    private static FastNoise islandHeightNoise;
    private static FastNoise ceilingNoise;
    private static FastNoise pillarSpawnNoise;
    private static FastNoise pillarJitterNoise;
    private static FastNoise bridgeNoise;
    private static FastNoise spikeNoise;

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

            upperChamber1 = new FastNoise(seed1 + 3333);
            upperChamber1.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            upperChamber1.SetFrequency(0.048f);
            upperChamber1.SetFractalOctaves(1);
            upperChamber1.SetFractalGain(0.3f);

            upperChamber2 = new FastNoise(seed2 + 4444);
            upperChamber2.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            upperChamber2.SetFrequency(0.048f);
            upperChamber2.SetFractalOctaves(1);
            upperChamber2.SetFractalGain(0.3f);

            upperTunnel1 = new FastNoise(seed1 + 5555);
            upperTunnel1.SetNoiseType(FastNoise.NoiseType.Simplex);
            upperTunnel1.SetFrequency(0.026f);

            upperTunnel2 = new FastNoise(seed2 + 6666);
            upperTunnel2.SetNoiseType(FastNoise.NoiseType.Simplex);
            upperTunnel2.SetFrequency(0.026f);

            lowerCavern1 = new FastNoise(seed1 + 7777);
            lowerCavern1.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            lowerCavern1.SetFrequency(0.009f);
            lowerCavern1.SetFractalOctaves(1);
            lowerCavern1.SetFractalGain(0.3f);

            lowerCavern2 = new FastNoise(seed2 + 8888);
            lowerCavern2.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            lowerCavern2.SetFrequency(0.009f);
            lowerCavern2.SetFractalOctaves(1);
            lowerCavern2.SetFractalGain(0.3f);

            floorIslandNoise = new FastNoise(seed2 + 9300);
            floorIslandNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            floorIslandNoise.SetFrequency(0.035f);

            islandHeightNoise = new FastNoise(seed1 + 9350);
            islandHeightNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            islandHeightNoise.SetFrequency(0.055f);

            ceilingNoise = new FastNoise(seed2 + 9370);
            ceilingNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            ceilingNoise.SetFrequency(0.04f);

            pillarSpawnNoise = new FastNoise(seed1 + 9100);
            pillarSpawnNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            pillarSpawnNoise.SetFrequency(1.0f);

            pillarJitterNoise = new FastNoise(seed2 + 9200);
            pillarJitterNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            pillarJitterNoise.SetFrequency(1.0f);

            bridgeNoise = new FastNoise(seed1 + 9400);
            bridgeNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            bridgeNoise.SetFrequency(0.038f);

            spikeNoise = new FastNoise(seed2 + 9500);
            spikeNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            spikeNoise.SetFrequency(0.07f);

            noiseInitialized = true;
        }
    }

    private static float columnStrength(int worldX, int worldZ, float heightFrac) {
        int spacing = COLUMN_SPACING;
        int cellX = Math.floorDiv(worldX, spacing);
        int cellZ = Math.floorDiv(worldZ, spacing);
        float best = 0.0f;

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                int cx = cellX + dx;
                int cz = cellZ + dz;
                if (pillarSpawnNoise.GetNoise(cx * 17.3f, cz * 31.7f) < 0.25f) continue;

                float jx = pillarJitterNoise.GetNoise(cx * 13.1f, cz * 19.7f);
                float jz = pillarJitterNoise.GetNoise(cx * 23.9f + 50.0f, cz * 11.3f);
                float centerX = cx * spacing + spacing * 0.5f + jx * (spacing * 0.22f);
                float centerZ = cz * spacing + spacing * 0.5f + jz * (spacing * 0.22f);
                float dist = MathHelper.sqrt((worldX - centerX) * (worldX - centerX) + (worldZ - centerZ) * (worldZ - centerZ));
                float radius = COLUMN_RADIUS * (1.0f - 0.22f * heightFrac);
                if (dist <= radius) {
                    float strength = 1.0f - (dist / Math.max(0.001f, radius));
                    if (strength > best) best = strength;
                }
            }
        }
        return best;
    }

    /** Ceiling Y around -25 with ±1–2 variation (never opens a pit into upper). */
    private static int ceilingYAt(int worldX, int worldZ) {
        float n = ceilingNoise.GetNoise(worldX, worldZ);
        int bump = n > 0.35f ? 2 : (n > 0.05f ? 1 : (n < -0.35f ? -1 : 0));
        // Nominal roof underside at -25; bump raises underside (shorter cavern) or lowers slightly
        return MathHelper.clamp(-25 + bump, -27, -23);
    }

    @Inject(method = "generate(IILnet/minecraft/world/chunk/ChunkPrimer;[Lnet/minecraft/world/biome/Biome;)V", remap = false, at = @At("HEAD"), cancellable = true)
    private void onGenerateDepthsBetterCaves(int chunkX, int chunkZ, ChunkPrimer primer, Biome[] biomes, CallbackInfo ci) {
        if (!(ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule && ArcanaQuestTweaksConfig.depthsModule.enableBetterCavesNegativeY)) {
            return;
        }
        if (primer == null) return;

        int minY = ArcanaQuestTweaksConfig.depthsModule.minWorldY;
        if (minY >= 0) return;

        initNoiseIfNeeded(this.capturedWorld != null ? Reflect.getSeed(this.capturedWorld) : 1337L);

        if (!loggedOnce) {
            LOGGER.info("[AQ-DEPTHS] Primer authority: upper tunnels, lower≤{}, decor in primer", LOWER_MAX_Y);
            loggedOnce = true;
        }

        int breachTop = Math.min(MAX_PRIMER_CARVE_Y, 4);
        int breachBottom = -25;
        int upperTop = Math.min(MAX_PRIMER_CARVE_Y, -5);
        int upperBottom = -25;
        int lowerBottom = -60;
        int bedrockTop = minY + 3;
        int lavaLevel = -55;

        IBlockState bedrockState = Reflect.getBedrockState();
        IBlockState airState = Reflect.getAirState();
        IBlockState lavaState = Reflect.getLavaState();
        IBlockState deepslateState = Reflect.getDeepslateState();
        net.minecraft.block.Block airBlock = Reflect.getAirBlock();
        net.minecraft.block.Block bedrockBlock = Reflect.getBedrockBlock();
        net.minecraft.block.Block lavaBlock = Reflect.getLavaBlock();

        if (deepslateState == null) {
            LOGGER.error("[AQ-DEPTHS] Deepslate NULL in primer — decor skipped");
        }

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;

        int breachHeight = Math.max(0, breachTop - breachBottom + 1);
        int upperHeight = Math.max(0, upperTop - upperBottom + 1);
        int lowerHeight = Math.max(0, LOWER_MAX_Y - lowerBottom + 1);
        int loopMaxY = Math.min(MAX_PRIMER_CARVE_Y, Math.max(breachTop, upperTop));

        for (int localX = 0; localX < 16; ++localX) {
            int worldX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int worldZ = startZ + localZ;

                for (int y = minY; y <= bedrockTop; ++y) {
                    Reflect.setBlockState(primer, localX, y, localZ, bedrockState);
                }

                float floorVal = floorIslandNoise.GetNoise(worldX, worldZ);
                float islandH = islandHeightNoise.GetNoise(worldX * 0.7f, worldZ * 0.7f);
                float bridgeVal = bridgeNoise.GetNoise(worldX, worldZ);
                float spikeVal = spikeNoise.GetNoise(worldX, worldZ);
                int ceilY = ceilingYAt(worldX, worldZ);

                float[] breach1 = new float[Math.max(1, breachHeight)];
                float[] breach2 = new float[Math.max(1, breachHeight)];
                if (breachHeight > 0) {
                    sampleDual(breachNoise1, breachNoise2, worldX, worldZ, breachBottom, breachTop, 1.0f, 0.70f, breach1, breach2);
                    applyTopDownYAdjust(breach1, breach2, breachHeight, BREACH_THRESHOLD);
                }

                float[] upperC1 = new float[Math.max(1, upperHeight)];
                float[] upperC2 = new float[Math.max(1, upperHeight)];
                float[] upperT1 = new float[Math.max(1, upperHeight)];
                float[] upperT2 = new float[Math.max(1, upperHeight)];
                boolean columnHasTunnel = false;
                if (upperHeight > 0) {
                    sampleDual(upperChamber1, upperChamber2, worldX, worldZ, upperBottom, upperTop, 1.0f, 2.8f, upperC1, upperC2);
                    sampleDual(upperTunnel1, upperTunnel2, worldX, worldZ, upperBottom, upperTop, 1.0f, 0.90f, upperT1, upperT2);
                    for (int i = 0; i < upperHeight; ++i) {
                        if (Math.abs(upperT1[i]) < UPPER_TUNNEL_WIDTH && Math.abs(upperT2[i]) < UPPER_TUNNEL_WIDTH) {
                            columnHasTunnel = true;
                            break;
                        }
                    }
                }

                float[] lower1 = new float[Math.max(1, lowerHeight)];
                float[] lower2 = new float[Math.max(1, lowerHeight)];
                if (lowerHeight > 0) {
                    sampleDual(lowerCavern1, lowerCavern2, worldX, worldZ, lowerBottom, LOWER_MAX_Y, 0.55f, 0.45f, lower1, lower2);
                }

                // --- Carve ---
                for (int y = bedrockTop + 1; y <= loopMaxY; ++y) {
                    IBlockState currentState = Reflect.getBlockState(primer, localX, y, localZ);
                    net.minecraft.block.Block currentBlock = Reflect.getBlock(currentState);
                    if (currentState == null || (airBlock != null && currentBlock == airBlock) || (bedrockBlock != null && currentBlock == bedrockBlock)) {
                        continue;
                    }

                    boolean carve = false;

                    // Breach roots (mouths at Y0 owned by BC companion + chunk reinforce)
                    if (breachHeight > 0 && y >= breachBottom && y <= breachTop) {
                        int idx = y - breachBottom;
                        if (idx >= 0 && idx < breachHeight && breach1[idx] > BREACH_THRESHOLD && breach2[idx] > BREACH_THRESHOLD) {
                            carve = true;
                        }
                    }

                    // Upper tunnels (~4–5 wide) + vertical thickness
                    if (!carve && upperHeight > 0 && y >= upperBottom && y <= upperTop) {
                        int idx = y - upperBottom;
                        if (idx >= 0 && idx < upperHeight) {
                            boolean tunnelHere = Math.abs(upperT1[idx]) < UPPER_TUNNEL_WIDTH
                                    && Math.abs(upperT2[idx]) < UPPER_TUNNEL_WIDTH;
                            // Also carve if a nearby Y is tunnel core (makes ~4–5 tall)
                            boolean nearCore = false;
                            for (int dy = -2; dy <= 2 && !nearCore; ++dy) {
                                int j = idx + dy;
                                if (j >= 0 && j < upperHeight
                                        && Math.abs(upperT1[j]) < UPPER_TUNNEL_WIDTH * 0.75f
                                        && Math.abs(upperT2[j]) < UPPER_TUNNEL_WIDTH * 0.75f) {
                                    nearCore = Math.abs(dy) <= 2;
                                }
                            }
                            if (tunnelHere || nearCore) {
                                carve = true;
                            } else if (columnHasTunnel && upperC1[idx] * upperC2[idx] > UPPER_CHAMBER_THRESHOLD) {
                                float mid = (upperTop + upperBottom) * 0.5f;
                                if (Math.abs(y - mid) <= 4.0f) {
                                    carve = true;
                                }
                            }
                        }
                    }

                    // Lower underworld — hard stop at LOWER_MAX_Y, below scalloped ceiling
                    if (!carve && lowerHeight > 0 && y >= lowerBottom && y <= LOWER_MAX_Y && y < ceilY) {
                        int idx = y - lowerBottom;
                        if (idx >= 0 && idx < lowerHeight) {
                            float openThr = LOWER_CAVERN_OPEN;
                            if (y >= ceilY - 4) {
                                float frac = (float) (ceilY - y) / 4.0f;
                                openThr *= MathHelper.clamp(0.30f + 0.70f * frac, 0.25f, 1.0f);
                            }
                            if (y < lavaLevel + 5) {
                                float frac = (float) (y - lowerBottom) / (float) Math.max(1, (lavaLevel + 5) - lowerBottom);
                                openThr *= MathHelper.clamp(0.40f + 0.60f * frac, 0.30f, 1.0f);
                            }
                            if (lower1[idx] * lower2[idx] < openThr) {
                                carve = true;
                            }
                        }
                    }

                    // Island pads with +0..2 height
                    int islandExtra = 0;
                    if (floorVal > 0.06f) {
                        islandExtra = islandH > 0.20f ? 2 : (islandH > -0.05f ? 1 : 0);
                    }
                    boolean islandSolid = floorVal > 0.06f && y >= lavaLevel && y <= lavaLevel + islandExtra;

                    if (islandSolid) {
                        if (deepslateState != null) {
                            Reflect.setBlockState(primer, localX, y, localZ, deepslateState);
                        }
                    } else if (carve) {
                        if (y <= lavaLevel) {
                            Reflect.setBlockState(primer, localX, y, localZ, lavaState);
                        } else {
                            Reflect.setBlockState(primer, localX, y, localZ, airState);
                        }
                    }
                }

                // Ensure solid roof shell at ceilY .. -23 (no pit into upper)
                if (deepslateState != null) {
                    for (int y = ceilY; y <= -23; ++y) {
                        IBlockState st = Reflect.getBlockState(primer, localX, y, localZ);
                        net.minecraft.block.Block b = Reflect.getBlock(st);
                        if (b != null && airBlock != null && (b == airBlock || (lavaBlock != null && b == lavaBlock))) {
                            Reflect.setBlockState(primer, localX, y, localZ, deepslateState);
                        }
                    }
                }

                // --- Decor (primer) ---
                if (deepslateState == null) continue;

                // Columns from -60 up to ceilY
                for (int y = lowerBottom; y <= ceilY; ++y) {
                    float heightFrac = (float) (y - lowerBottom) / (float) Math.max(1, ceilY - lowerBottom);
                    if (columnStrength(worldX, worldZ, heightFrac) > 0.18f) {
                        Reflect.setBlockState(primer, localX, y, localZ, deepslateState);
                    }
                }

                // Stalagmites from island / solid floor
                int floorY = -1;
                for (int y = lowerBottom; y < ceilY - 2; ++y) {
                    net.minecraft.block.Block b = Reflect.getBlock(Reflect.getBlockState(primer, localX, y, localZ));
                    net.minecraft.block.Block ab = Reflect.getBlock(Reflect.getBlockState(primer, localX, y + 1, localZ));
                    boolean solidOrLava = b != null && airBlock != null && b != airBlock && (bedrockBlock == null || b != bedrockBlock);
                    boolean openAbove = ab != null && airBlock != null && ab == airBlock;
                    if (solidOrLava && openAbove) {
                        floorY = y;
                    }
                }
                if (floorY >= 0 && spikeVal > 0.22f) {
                    int miteH = MathHelper.clamp(5 + (int) ((spikeVal - 0.22f) * 20.0f), 5, 16);
                    for (int y = floorY + 1; y <= floorY + miteH && y < ceilY; ++y) {
                        net.minecraft.block.Block b = Reflect.getBlock(Reflect.getBlockState(primer, localX, y, localZ));
                        if (b != null && airBlock != null && (b == airBlock || (lavaBlock != null && b == lavaBlock))) {
                            Reflect.setBlockState(primer, localX, y, localZ, deepslateState);
                        }
                    }
                }

                // Stalactites from ceiling
                if (spikeVal < -0.20f) {
                    int titeH = MathHelper.clamp(5 + (int) ((-spikeVal - 0.20f) * 20.0f), 5, 16);
                    for (int y = ceilY - 1; y >= ceilY - titeH && y > lowerBottom; --y) {
                        net.minecraft.block.Block b = Reflect.getBlock(Reflect.getBlockState(primer, localX, y, localZ));
                        if (b != null && airBlock != null && b == airBlock) {
                            Reflect.setBlockState(primer, localX, y, localZ, deepslateState);
                        } else if (b != null && airBlock != null && b != airBlock) {
                            break;
                        }
                    }
                }

                // Arches / ledges over lava
                if (floorVal < 0.04f && bridgeVal > 0.12f) {
                    int archMid = lavaLevel + 7 + (int) (bridgeVal * 5.0f);
                    int archHalf = MathHelper.clamp(2 + (int) ((bridgeVal - 0.12f) * 10.0f), 2, 4);
                    for (int y = archMid - archHalf; y <= archMid + archHalf; ++y) {
                        if (y <= lavaLevel || y >= ceilY) continue;
                        net.minecraft.block.Block b = Reflect.getBlock(Reflect.getBlockState(primer, localX, y, localZ));
                        if (b != null && airBlock != null && (b == airBlock || (lavaBlock != null && b == lavaBlock))) {
                            Reflect.setBlockState(primer, localX, y, localZ, deepslateState);
                        }
                    }
                }
            }
        }

        ci.cancel();
    }

    private static void applyTopDownYAdjust(float[] v1, float[] v2, int height, float threshold) {
        for (int idx = height - 1; idx >= 0; --idx) {
            if (v1[idx] >= threshold && v2[idx] >= threshold) {
                if (idx - 1 >= 0) {
                    v1[idx - 1] = 0.10f * v1[idx - 1] + 0.90f * v1[idx];
                    v2[idx - 1] = 0.10f * v2[idx - 1] + 0.90f * v2[idx];
                }
                if (idx - 2 >= 0) {
                    v1[idx - 2] = 0.30f * v1[idx - 2] + 0.70f * v1[idx];
                    v2[idx - 2] = 0.30f * v2[idx - 2] + 0.70f * v2[idx];
                }
            }
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
                out1[idx] = n1.GetNoise(worldX * xzComp, y * yComp, worldZ * xzComp);
                out2[idx] = n2.GetNoise(worldX * xzComp, y * yComp, worldZ * xzComp);
            }
        }
        int lastIdx = height - 1;
        if (lastIdx % 4 != 0) {
            out1[lastIdx] = n1.GetNoise(worldX * xzComp, yTop * yComp, worldZ * xzComp);
            out2[lastIdx] = n2.GetNoise(worldX * xzComp, yTop * yComp, worldZ * xzComp);
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
