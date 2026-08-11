package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.depths.UpperTunnelNetwork;
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

    /** Cuboid upper tunnels via UpperTunnelNetwork; lower cavern open threshold */
    private static final float LOWER_CAVERN_OPEN = 0.45f;

    /** Landmass vs lava lakes/streams (majority land) */
    private static final float LAND_THR = -0.12f;
    private static final float LAVA_CHANNEL_THR = -0.25f;

    private static final float COLUMN_RADIUS = 3.75f;
    private static final int COLUMN_SPACING = 24;

    /** Natural land bridges (~75% of prior rate) */
    private static final int BRIDGE_SPACING = 16;
    private static final float BRIDGE_HALF_WIDTH = 1.2f;
    private static final int BRIDGE_HALF_SPAN = 16;
    private static final float BRIDGE_SPAWN_MIN = 0.28f;
    private static final float BRIDGE_RISE_MIN = 4.0f;
    private static final float BRIDGE_RISE_MAX = 6.0f;

    private static final int LOWER_MAX_Y = -26;
    private static final int MAX_PRIMER_LOOP_Y = 4;

    /** Short floor spikes — rarer than stalactites */
    private static final float FLOOR_SPIKE_THR = 0.52f;

    private static FastNoise lowerCavern1;
    private static FastNoise lowerCavern2;
    private static FastNoise floorIslandNoise;
    private static FastNoise islandHeightNoise;
    private static FastNoise ceilingNoise;
    private static FastNoise pillarSpawnNoise;
    private static FastNoise pillarJitterNoise;
    private static FastNoise bridgeSpawnNoise;
    private static FastNoise bridgeJitterNoise;
    private static FastNoise bridgeDirNoise;
    private static FastNoise bridgeRiseNoise;
    private static FastNoise bridgeEdgeNoise;
    private static FastNoise spikeNoise;

    private static boolean noiseInitialized = false;
    private static boolean loggedOnce = false;

    private World capturedWorld;

    @Inject(method = "<init>(Lnet/minecraft/world/World;)V", remap = false, at = @At("RETURN"))
    private void onInitDepthsBetterCaves(World world, CallbackInfo ci) {
        this.capturedWorld = world;
        long seed = world != null ? Reflect.getSeed(world) : 1337L;
        initNoiseIfNeeded(seed);
        UpperTunnelNetwork.init(seed);
    }

    private static synchronized void initNoiseIfNeeded(long worldSeed) {
        if (!noiseInitialized) {
            int seed1 = (int) (worldSeed & 0xFFFF);
            int seed2 = (int) ((worldSeed >> 16) & 0xFFFF);

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
            floorIslandNoise.SetFrequency(0.032f);

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

            bridgeSpawnNoise = new FastNoise(seed1 + 9400);
            bridgeSpawnNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            bridgeSpawnNoise.SetFrequency(1.0f);

            bridgeJitterNoise = new FastNoise(seed2 + 9410);
            bridgeJitterNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            bridgeJitterNoise.SetFrequency(1.0f);

            bridgeDirNoise = new FastNoise(seed1 + 9420);
            bridgeDirNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            bridgeDirNoise.SetFrequency(1.0f);

            bridgeRiseNoise = new FastNoise(seed2 + 9430);
            bridgeRiseNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            bridgeRiseNoise.SetFrequency(1.0f);

            bridgeEdgeNoise = new FastNoise(seed1 + 9440);
            bridgeEdgeNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            bridgeEdgeNoise.SetFrequency(0.15f);

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
                // Wider at top & bottom by ~1–2 blocks radius vs mid-shaft
                float midFactor = 1.0f - 4.0f * (heightFrac - 0.5f) * (heightFrac - 0.5f); // 1 at mid, 0 at ends
                float radius = COLUMN_RADIUS + 1.35f * (1.0f - midFactor);
                if (dist <= radius) {
                    float strength = 1.0f - (dist / Math.max(0.001f, radius));
                    if (strength > best) best = strength;
                }
            }
        }
        return best;
    }

    private static boolean isLand(float floorVal) {
        return floorVal > LAND_THR;
    }

    private static boolean isLavaChannel(float floorVal) {
        return floorVal < LAVA_CHANNEL_THR;
    }

    /**
     * Natural land-bridge ridge: returns top deck Y, or Integer.MIN_VALUE if none.
     * Caller fills solid from near lava up to deck (ridge), not a floating staircase.
     */
    private static int bridgeDeckY(int worldX, int worldZ, int lavaLevel) {
        int spacing = BRIDGE_SPACING;
        int cellX = Math.floorDiv(worldX, spacing);
        int cellZ = Math.floorDiv(worldZ, spacing);
        int bestDeck = Integer.MIN_VALUE;

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                int cx = cellX + dx;
                int cz = cellZ + dz;
                if (bridgeSpawnNoise.GetNoise(cx * 19.1f, cz * 27.3f) < BRIDGE_SPAWN_MIN) continue;

                float jx = bridgeJitterNoise.GetNoise(cx * 11.7f, cz * 29.3f);
                float jz = bridgeJitterNoise.GetNoise(cx * 31.1f + 40.0f, cz * 13.9f);
                int centerX = Math.round(cx * spacing + spacing * 0.5f + jx * (spacing * 0.15f));
                int centerZ = Math.round(cz * spacing + spacing * 0.5f + jz * (spacing * 0.15f));

                float dir = bridgeDirNoise.GetNoise(cx * 7.3f, cz * 17.9f);
                boolean alongX = dir >= 0.0f;

                int endAx = alongX ? centerX - BRIDGE_HALF_SPAN : centerX;
                int endAz = alongX ? centerZ : centerZ - BRIDGE_HALF_SPAN;
                int endBx = alongX ? centerX + BRIDGE_HALF_SPAN : centerX;
                int endBz = alongX ? centerZ : centerZ + BRIDGE_HALF_SPAN;

                float endA = floorIslandNoise.GetNoise(endAx, endAz);
                float endB = floorIslandNoise.GetNoise(endBx, endBz);
                float mid = floorIslandNoise.GetNoise(centerX, centerZ);
                if (!isLand(endA) || !isLand(endB)) continue;
                if (!isLavaChannel(mid)) continue;

                float dxw = worldX - centerX;
                float dzw = worldZ - centerZ;
                float along = alongX ? dxw : dzw;
                float across = alongX ? dzw : dxw;
                if (Math.abs(along) > BRIDGE_HALF_SPAN + 0.5f) continue;

                float edgeJitter = bridgeEdgeNoise.GetNoise(worldX * 0.8f, worldZ * 0.8f) * 0.45f;
                float halfW = BRIDGE_HALF_WIDTH + edgeJitter;
                // Mild widen only at very ends so bridge merges into land
                float endBlend = MathHelper.clamp(Math.abs(along) / (float) BRIDGE_HALF_SPAN, 0.0f, 1.0f);
                halfW += endBlend * endBlend * 0.45f;
                if (Math.abs(across) > halfW) continue;

                // Wide high crest: most of span stays near peak; short ramps at ends
                float u = MathHelper.clamp(along / (float) BRIDGE_HALF_SPAN, -1.0f, 1.0f);
                float absU = Math.abs(u);
                // 1 - |u|^2.4 → flat mid plateau, steeper near ends
                float arch = 1.0f - (float) Math.pow(absU, 2.4);
                float riseN = bridgeRiseNoise.GetNoise(cx * 5.1f, cz * 9.7f) * 0.5f + 0.5f;
                float rise = BRIDGE_RISE_MIN + riseN * (BRIDGE_RISE_MAX - BRIDGE_RISE_MIN);
                float topJitter = bridgeEdgeNoise.GetNoise(worldX * 1.3f + 9.0f, worldZ * 1.3f) * 0.45f;
                int endY = lavaLevel + 1;
                int deckY = Math.round(endY + rise * arch + topJitter);
                if (deckY > bestDeck) bestDeck = deckY;
            }
        }
        return bestDeck;
    }

    /** How much solid to fill under the deck (thicker near ends = land-bridge piers). */
    private static int bridgeFillBottom(int worldX, int worldZ, int lavaLevel, int deckY) {
        int spacing = BRIDGE_SPACING;
        int cellX = Math.floorDiv(worldX, spacing);
        int cellZ = Math.floorDiv(worldZ, spacing);
        float bestU = 0.0f;
        boolean found = false;

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                int cx = cellX + dx;
                int cz = cellZ + dz;
                if (bridgeSpawnNoise.GetNoise(cx * 19.1f, cz * 27.3f) < BRIDGE_SPAWN_MIN) continue;

                float jx = bridgeJitterNoise.GetNoise(cx * 11.7f, cz * 29.3f);
                float jz = bridgeJitterNoise.GetNoise(cx * 31.1f + 40.0f, cz * 13.9f);
                int centerX = Math.round(cx * spacing + spacing * 0.5f + jx * (spacing * 0.15f));
                int centerZ = Math.round(cz * spacing + spacing * 0.5f + jz * (spacing * 0.15f));
                float dir = bridgeDirNoise.GetNoise(cx * 7.3f, cz * 17.9f);
                boolean alongX = dir >= 0.0f;
                float along = alongX ? (worldX - centerX) : (worldZ - centerZ);
                float across = alongX ? (worldZ - centerZ) : (worldX - centerX);
                if (Math.abs(along) > BRIDGE_HALF_SPAN + 0.5f) continue;
                if (Math.abs(across) > BRIDGE_HALF_WIDTH + 0.8f) continue;
                float u = Math.abs(MathHelper.clamp(along / (float) BRIDGE_HALF_SPAN, -1.0f, 1.0f));
                if (!found || u > bestU) {
                    bestU = u;
                    found = true;
                }
            }
        }
        if (!found) return lavaLevel + 1;
        // Arched underside: thin at mid (2–3), curves down toward ends to meet land
        // undersideRise follows same crest curve as deck so void under mid is smooth
        float arch = 1.0f - (float) Math.pow(bestU, 2.4); // 1 at mid, 0 at ends
        int thickness = 2 + Math.round(bestU * 1.5f); // 2 mid → ~3–4 near ends
        int underside = deckY - thickness - Math.round((1.0f - arch) * Math.max(0, deckY - (lavaLevel + 3)) * 0.85f);
        // Soften with slight noise
        underside += Math.round(bridgeEdgeNoise.GetNoise(worldX * 0.9f, worldZ * 0.9f) * 0.6f);
        return MathHelper.clamp(underside, lavaLevel + 1, deckY - 2);
    }

    private static int ceilingYAt(int worldX, int worldZ) {
        float n = ceilingNoise.GetNoise(worldX, worldZ);
        int bump = n > 0.35f ? 2 : (n > 0.05f ? 1 : (n < -0.35f ? -1 : 0));
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

        long seed = this.capturedWorld != null ? Reflect.getSeed(this.capturedWorld) : 1337L;
        initNoiseIfNeeded(seed);
        UpperTunnelNetwork.init(seed);

        if (!loggedOnce) {
            LOGGER.info("[AQ-DEPTHS] Primer: BC-style upper worms + chambers, landmass floor");
            loggedOnce = true;
        }

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

        int lowerHeight = Math.max(0, LOWER_MAX_Y - lowerBottom + 1);
        int loopMaxY = MAX_PRIMER_LOOP_Y;

        for (int localX = 0; localX < 16; ++localX) {
            int worldX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int worldZ = startZ + localZ;

                for (int y = minY; y <= bedrockTop; ++y) {
                    Reflect.setBlockState(primer, localX, y, localZ, bedrockState);
                }

                float floorVal = floorIslandNoise.GetNoise(worldX, worldZ);
                float islandH = islandHeightNoise.GetNoise(worldX * 0.7f, worldZ * 0.7f);
                float spikeVal = spikeNoise.GetNoise(worldX, worldZ);
                int ceilY = ceilingYAt(worldX, worldZ);
                boolean land = isLand(floorVal);
                boolean lavaChannel = isLavaChannel(floorVal);
                int landExtra = 0;
                if (land) {
                    landExtra = islandH > 0.20f ? 2 : (islandH > -0.05f ? 1 : 0);
                }
                int landSurfaceY = lavaLevel + landExtra;

                boolean lowerAdit = UpperTunnelNetwork.breachesLower(worldX, worldZ);
                int archDeckY = bridgeDeckY(worldX, worldZ, lavaLevel);

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

                    // Sloping cuboid tunnels + chambers (+ short +Y neck / lower adit via floor grade)
                    if (UpperTunnelNetwork.carveUpperAt(worldX, worldZ, y)) {
                        carve = true;
                    }

                    // Lower cavern air (above land/lava surface)
                    if (!carve && lowerHeight > 0 && y >= lowerBottom && y <= LOWER_MAX_Y && (y < ceilY || lowerAdit)) {
                        if (land && y <= landSurfaceY) {
                            // keep solid
                        } else {
                            int idx = y - lowerBottom;
                            if (idx >= 0 && idx < lowerHeight) {
                                float openThr = LOWER_CAVERN_OPEN;
                                if (!lowerAdit && y >= ceilY - 4) {
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
                    }

                    // Landmass rooted to bedrock
                    if (land && y >= bedrockTop + 1 && y <= landSurfaceY) {
                        if (deepslateState != null) {
                            Reflect.setBlockState(primer, localX, y, localZ, deepslateState);
                        }
                    } else if (carve) {
                        if (y <= lavaLevel && !land) {
                            Reflect.setBlockState(primer, localX, y, localZ, lavaState);
                        } else {
                            Reflect.setBlockState(primer, localX, y, localZ, airState);
                        }
                    }
                }

                // Ensure landmass fill even where carve didn't run (solid default)
                if (land && deepslateState != null) {
                    for (int y = bedrockTop + 1; y <= landSurfaceY; ++y) {
                        Reflect.setBlockState(primer, localX, y, localZ, deepslateState);
                    }
                }

                // Lava lakes/streams in channels (fill open cells at/below lava level)
                if (!land && lavaChannel) {
                    for (int y = bedrockTop + 1; y <= lavaLevel; ++y) {
                        net.minecraft.block.Block b = Reflect.getBlock(Reflect.getBlockState(primer, localX, y, localZ));
                        if (b != null && airBlock != null && b == airBlock) {
                            Reflect.setBlockState(primer, localX, y, localZ, lavaState);
                        }
                    }
                }

                // Roof shell — leave open where sloping tunnels breach into lower
                if (deepslateState != null) {
                    if (lowerAdit) {
                        for (int y = -28; y <= -22; ++y) {
                            net.minecraft.block.Block b = Reflect.getBlock(Reflect.getBlockState(primer, localX, y, localZ));
                            if (b != null && airBlock != null && b != airBlock
                                    && (bedrockBlock == null || b != bedrockBlock)
                                    && UpperTunnelNetwork.carveTunnelAt(worldX, worldZ, y)) {
                                Reflect.setBlockState(primer, localX, y, localZ, airState);
                            }
                        }
                    } else {
                        for (int y = ceilY; y <= -23; ++y) {
                            IBlockState st = Reflect.getBlockState(primer, localX, y, localZ);
                            net.minecraft.block.Block b = Reflect.getBlock(st);
                            if (b != null && airBlock != null && (b == airBlock || (lavaBlock != null && b == lavaBlock))) {
                                // Don't reseal active tunnel cuboid cutting the shell
                                if (UpperTunnelNetwork.carveTunnelAt(worldX, worldZ, y)) continue;
                                Reflect.setBlockState(primer, localX, y, localZ, deepslateState);
                            }
                        }
                    }
                }

                if (deepslateState == null) continue;

                // Columns
                for (int y = lowerBottom; y <= ceilY; ++y) {
                    if (UpperTunnelNetwork.carveTunnelAt(worldX, worldZ, y)) continue;
                    float heightFrac = (float) (y - lowerBottom) / (float) Math.max(1, ceilY - lowerBottom);
                    if (columnStrength(worldX, worldZ, heightFrac) > 0.18f) {
                        Reflect.setBlockState(primer, localX, y, localZ, deepslateState);
                    }
                }

                // Short floor stalagmites on land (4–5), much rarer than stalactites
                if (land && spikeVal > FLOOR_SPIKE_THR) {
                    int surface = landSurfaceY;
                    net.minecraft.block.Block aboveSurf = Reflect.getBlock(Reflect.getBlockState(primer, localX, surface + 1, localZ));
                    if (aboveSurf != null && airBlock != null && aboveSurf == airBlock) {
                        int miteH = spikeVal > 0.72f ? 5 : 4;
                        for (int y = surface + 1; y <= surface + miteH && y < ceilY; ++y) {
                            net.minecraft.block.Block b = Reflect.getBlock(Reflect.getBlockState(primer, localX, y, localZ));
                            if (b != null && airBlock != null && b == airBlock) {
                                Reflect.setBlockState(primer, localX, y, localZ, deepslateState);
                            } else {
                                break;
                            }
                        }
                    }
                }

                // Stalactites from ceiling (more common than floor spikes)
                if (spikeVal < -0.15f) {
                    int titeH = MathHelper.clamp(5 + (int) ((-spikeVal - 0.15f) * 18.0f), 5, 16);
                    for (int y = ceilY - 1; y >= ceilY - titeH && y > landSurfaceY; --y) {
                        net.minecraft.block.Block b = Reflect.getBlock(Reflect.getBlockState(primer, localX, y, localZ));
                        if (b != null && airBlock != null && b == airBlock) {
                            Reflect.setBlockState(primer, localX, y, localZ, deepslateState);
                        } else if (b != null && airBlock != null && b != airBlock) {
                            break;
                        }
                    }
                }

                // Natural land-bridge ridge (solid fill under smooth arch)
                int archFillBottom = Integer.MIN_VALUE;
                if (archDeckY != Integer.MIN_VALUE && archDeckY > lavaLevel && archDeckY < ceilY - 2) {
                    archFillBottom = bridgeFillBottom(worldX, worldZ, lavaLevel, archDeckY);
                    for (int y = archFillBottom; y <= archDeckY; ++y) {
                        if (y <= lavaLevel || y >= ceilY) continue;
                        net.minecraft.block.Block b = Reflect.getBlock(Reflect.getBlockState(primer, localX, y, localZ));
                        if (b != null && airBlock != null && (b == airBlock || (lavaBlock != null && b == lavaBlock))) {
                            Reflect.setBlockState(primer, localX, y, localZ, deepslateState);
                        }
                    }
                }

                // Orphan floater cleanup
                for (int y = lavaLevel + 3; y <= ceilY - 3; ++y) {
                    if (archFillBottom != Integer.MIN_VALUE && y >= archFillBottom && y <= archDeckY) {
                        continue;
                    }
                    float hf = (float) (y - lowerBottom) / (float) Math.max(1, ceilY - lowerBottom);
                    if (columnStrength(worldX, worldZ, hf) > 0.15f) continue;
                    if (land && y <= landSurfaceY + 5) continue; // protect short floor spikes

                    net.minecraft.block.Block b = Reflect.getBlock(Reflect.getBlockState(primer, localX, y, localZ));
                    if (b == null || airBlock == null || b == airBlock) continue;
                    if (lavaBlock != null && b == lavaBlock) continue;
                    if (bedrockBlock != null && b == bedrockBlock) continue;

                    net.minecraft.block.Block below = Reflect.getBlock(Reflect.getBlockState(primer, localX, y - 1, localZ));
                    net.minecraft.block.Block below2 = Reflect.getBlock(Reflect.getBlockState(primer, localX, y - 2, localZ));
                    net.minecraft.block.Block above = Reflect.getBlock(Reflect.getBlockState(primer, localX, y + 1, localZ));
                    boolean openBelow = below != null && (below == airBlock || (lavaBlock != null && below == lavaBlock));
                    boolean openBelow2 = below2 != null && (below2 == airBlock || (lavaBlock != null && below2 == lavaBlock));
                    boolean openAbove = above != null && (above == airBlock || (lavaBlock != null && above == lavaBlock));

                    if (openBelow && openAbove) {
                        Reflect.setBlockState(primer, localX, y, localZ, airState);
                        continue;
                    }
                    if (openBelow && openBelow2) {
                        int run = 0;
                        for (int yy = y; yy < ceilY - 1 && run < 10; ++yy) {
                            net.minecraft.block.Block sb = Reflect.getBlock(Reflect.getBlockState(primer, localX, yy, localZ));
                            if (sb == null || sb == airBlock || (lavaBlock != null && sb == lavaBlock)) break;
                            if (bedrockBlock != null && sb == bedrockBlock) break;
                            run++;
                        }
                        if (run > 0 && run <= 8) {
                            net.minecraft.block.Block topAbove = Reflect.getBlock(Reflect.getBlockState(primer, localX, y + run, localZ));
                            boolean airAboveStack = topAbove != null && (topAbove == airBlock || (lavaBlock != null && topAbove == lavaBlock));
                            boolean touchesCeil = (y + run) >= ceilY - 1;
                            if (airAboveStack && !touchesCeil) {
                                for (int yy = y; yy < y + run; ++yy) {
                                    Reflect.setBlockState(primer, localX, yy, localZ, airState);
                                }
                            }
                        }
                    }
                }

                // Keep sloping lower-adit mouths open through the ceiling shell after decor
                if (lowerAdit && airState != null) {
                    for (int y = -28; y <= -22; ++y) {
                        if (!UpperTunnelNetwork.carveTunnelAt(worldX, worldZ, y)) continue;
                        net.minecraft.block.Block b = Reflect.getBlock(Reflect.getBlockState(primer, localX, y, localZ));
                        if (b != null && airBlock != null && b != airBlock
                                && (bedrockBlock == null || b != bedrockBlock)) {
                            Reflect.setBlockState(primer, localX, y, localZ, airState);
                        }
                    }
                }
            }
        }

        ci.cancel();
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
