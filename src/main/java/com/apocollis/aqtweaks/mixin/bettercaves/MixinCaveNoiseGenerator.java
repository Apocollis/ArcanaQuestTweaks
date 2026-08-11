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
    /** Sparse lava bridges — one candidate every ~28 blocks */
    private static final int BRIDGE_SPACING = 28;
    private static final float BRIDGE_HALF_WIDTH = 1.75f;
    /** Occasional tunnel→lower drop shafts */
    private static final int DROP_SPACING = 42;
    private static final float DROP_RADIUS = 1.6f;

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
    private static FastNoise bridgeSpawnNoise;
    private static FastNoise bridgeJitterNoise;
    private static FastNoise bridgeDirNoise;
    private static FastNoise dropSpawnNoise;
    private static FastNoise dropJitterNoise;
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

            bridgeSpawnNoise = new FastNoise(seed1 + 9400);
            bridgeSpawnNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            bridgeSpawnNoise.SetFrequency(1.0f);

            bridgeJitterNoise = new FastNoise(seed2 + 9410);
            bridgeJitterNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            bridgeJitterNoise.SetFrequency(1.0f);

            bridgeDirNoise = new FastNoise(seed1 + 9420);
            bridgeDirNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            bridgeDirNoise.SetFrequency(1.0f);

            dropSpawnNoise = new FastNoise(seed2 + 9600);
            dropSpawnNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            dropSpawnNoise.SetFrequency(1.0f);

            dropJitterNoise = new FastNoise(seed1 + 9610);
            dropJitterNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            dropJitterNoise.SetFrequency(1.0f);

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

    /**
     * Sparse bridge ribbon: cells every BRIDGE_SPACING; place a short strip (2–4 thick)
     * only near the cell center so islands connect without mid-air blob spam.
     */
    private static boolean bridgeSolidAt(int worldX, int worldZ) {
        int spacing = BRIDGE_SPACING;
        int cellX = Math.floorDiv(worldX, spacing);
        int cellZ = Math.floorDiv(worldZ, spacing);

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                int cx = cellX + dx;
                int cz = cellZ + dz;
                // ~1 bridge cell in ~3–4 candidates → roughly every 25–30 blocks when present
                if (bridgeSpawnNoise.GetNoise(cx * 19.1f, cz * 27.3f) < 0.42f) continue;

                float jx = bridgeJitterNoise.GetNoise(cx * 11.7f, cz * 29.3f);
                float jz = bridgeJitterNoise.GetNoise(cx * 31.1f + 40.0f, cz * 13.9f);
                float centerX = cx * spacing + spacing * 0.5f + jx * (spacing * 0.18f);
                float centerZ = cz * spacing + spacing * 0.5f + jz * (spacing * 0.18f);

                float dir = bridgeDirNoise.GetNoise(cx * 7.3f, cz * 17.9f);
                // Axis-aligned ribbon: X-span or Z-span
                boolean alongX = dir >= 0.0f;
                float halfLen = 5.5f + Math.abs(dir) * 2.5f;
                float dxw = worldX - centerX;
                float dzw = worldZ - centerZ;
                if (alongX) {
                    if (Math.abs(dxw) <= halfLen && Math.abs(dzw) <= BRIDGE_HALF_WIDTH) {
                        return true;
                    }
                } else {
                    if (Math.abs(dzw) <= halfLen && Math.abs(dxw) <= BRIDGE_HALF_WIDTH) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Distance into nearest drop-shaft disk, or -1 if none. */
    private static float dropShaftStrength(int worldX, int worldZ) {
        int spacing = DROP_SPACING;
        int cellX = Math.floorDiv(worldX, spacing);
        int cellZ = Math.floorDiv(worldZ, spacing);
        float best = -1.0f;

        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                int cx = cellX + dx;
                int cz = cellZ + dz;
                // Rare: only strong positive spawn noise
                if (dropSpawnNoise.GetNoise(cx * 41.3f, cz * 53.7f) < 0.55f) continue;

                float jx = dropJitterNoise.GetNoise(cx * 15.1f, cz * 21.7f);
                float jz = dropJitterNoise.GetNoise(cx * 33.9f + 60.0f, cz * 9.3f);
                float centerX = cx * spacing + spacing * 0.5f + jx * (spacing * 0.20f);
                float centerZ = cz * spacing + spacing * 0.5f + jz * (spacing * 0.20f);
                float dist = MathHelper.sqrt((worldX - centerX) * (worldX - centerX) + (worldZ - centerZ) * (worldZ - centerZ));
                if (dist <= DROP_RADIUS) {
                    float strength = 1.0f - (dist / DROP_RADIUS);
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
            LOGGER.info("[AQ-DEPTHS] Primer: sparse bridges={}, upper≤10, drop shafts", BRIDGE_SPACING);
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
                float spikeVal = spikeNoise.GetNoise(worldX, worldZ);
                int ceilY = ceilingYAt(worldX, worldZ);
                float dropStr = dropShaftStrength(worldX, worldZ);
                boolean onDropShaft = dropStr >= 0.0f;

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
                int tunnelCoreY = Integer.MAX_VALUE;
                if (upperHeight > 0) {
                    // Stronger Y compression → flatter chambers (≤8–10 tall)
                    sampleDual(upperChamber1, upperChamber2, worldX, worldZ, upperBottom, upperTop, 1.0f, 3.6f, upperC1, upperC2);
                    sampleDual(upperTunnel1, upperTunnel2, worldX, worldZ, upperBottom, upperTop, 1.0f, 1.15f, upperT1, upperT2);
                    for (int i = 0; i < upperHeight; ++i) {
                        if (Math.abs(upperT1[i]) < UPPER_TUNNEL_WIDTH && Math.abs(upperT2[i]) < UPPER_TUNNEL_WIDTH) {
                            columnHasTunnel = true;
                            int cy = upperBottom + i;
                            if (cy < tunnelCoreY) tunnelCoreY = cy;
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

                    // Upper tunnels (~4–5 wide, ~3–5 tall) + small chambers ≤8–10
                    if (!carve && upperHeight > 0 && y >= upperBottom && y <= upperTop) {
                        int idx = y - upperBottom;
                        if (idx >= 0 && idx < upperHeight) {
                            boolean tunnelHere = Math.abs(upperT1[idx]) < UPPER_TUNNEL_WIDTH
                                    && Math.abs(upperT2[idx]) < UPPER_TUNNEL_WIDTH;
                            // ±1 from core → tubes ~3–5 tall (not vaulted)
                            boolean nearCore = false;
                            for (int dy = -1; dy <= 1 && !nearCore; ++dy) {
                                int j = idx + dy;
                                if (j >= 0 && j < upperHeight
                                        && Math.abs(upperT1[j]) < UPPER_TUNNEL_WIDTH * 0.75f
                                        && Math.abs(upperT2[j]) < UPPER_TUNNEL_WIDTH * 0.75f) {
                                    nearCore = true;
                                }
                            }
                            // Soft roof bias: don't open chambers into Y≈-5
                            boolean nearRoof = y >= upperTop - 1;
                            if (tunnelHere || nearCore) {
                                carve = true;
                            } else if (!nearRoof && columnHasTunnel && tunnelCoreY != Integer.MAX_VALUE
                                    && Math.abs(y - tunnelCoreY) <= 4
                                    && upperC1[idx] * upperC2[idx] > UPPER_CHAMBER_THRESHOLD) {
                                carve = true;
                            }
                        }
                    }

                    // Tunnel-linked drop shaft: punch through upper floor / ceil shell into lower
                    if (!carve && onDropShaft && columnHasTunnel && tunnelCoreY != Integer.MAX_VALUE) {
                        if (y <= tunnelCoreY + 1 && y >= lowerBottom && y > lavaLevel) {
                            carve = true;
                        } else if (y <= lavaLevel && y >= lowerBottom) {
                            carve = true; // lava fill below
                        }
                    }

                    // Lower underworld — hard stop at LOWER_MAX_Y, below scalloped ceiling
                    // (drop shafts already carved through the shell above)
                    if (!carve && lowerHeight > 0 && y >= lowerBottom && y <= LOWER_MAX_Y && (y < ceilY || onDropShaft)) {
                        int idx = y - lowerBottom;
                        if (idx >= 0 && idx < lowerHeight) {
                            float openThr = LOWER_CAVERN_OPEN;
                            if (!onDropShaft && y >= ceilY - 4) {
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

                // Ensure solid roof shell at ceilY .. -23 (no pit into upper) — skip drop shafts
                if (deepslateState != null && !onDropShaft) {
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

                // Sparse bridges only (~every 25–30 blocks via cell grid)
                if (bridgeSolidAt(worldX, worldZ)) {
                    int archMid = lavaLevel + 8;
                    int archHalf = 2; // 5-block-thick ribbon
                    for (int y = archMid - archHalf; y <= archMid + archHalf; ++y) {
                        if (y <= lavaLevel || y >= ceilY) continue;
                        if (columnStrength(worldX, worldZ, (float) (y - lowerBottom) / (float) Math.max(1, ceilY - lowerBottom)) > 0.18f) {
                            continue; // don't thicken over columns oddly
                        }
                        net.minecraft.block.Block b = Reflect.getBlock(Reflect.getBlockState(primer, localX, y, localZ));
                        if (b != null && airBlock != null && (b == airBlock || (lavaBlock != null && b == lavaBlock))) {
                            Reflect.setBlockState(primer, localX, y, localZ, deepslateState);
                        }
                    }
                }

                // Orphan mid-air blob cleanup — floating 1–2 block scraps only (keep spikes / bridges / columns)
                boolean onBridge = bridgeSolidAt(worldX, worldZ);
                for (int y = lavaLevel + 3; y <= ceilY - 3; ++y) {
                    if (onBridge && y >= lavaLevel + 6 && y <= lavaLevel + 10) continue;
                    float hf = (float) (y - lowerBottom) / (float) Math.max(1, ceilY - lowerBottom);
                    if (columnStrength(worldX, worldZ, hf) > 0.15f) continue;

                    net.minecraft.block.Block b = Reflect.getBlock(Reflect.getBlockState(primer, localX, y, localZ));
                    if (b == null || airBlock == null || b == airBlock) continue;
                    if (lavaBlock != null && b == lavaBlock) continue;
                    if (bedrockBlock != null && b == bedrockBlock) continue;

                    net.minecraft.block.Block below = Reflect.getBlock(Reflect.getBlockState(primer, localX, y - 1, localZ));
                    net.minecraft.block.Block above = Reflect.getBlock(Reflect.getBlockState(primer, localX, y + 1, localZ));
                    boolean openBelow = below != null && (below == airBlock || (lavaBlock != null && below == lavaBlock));
                    boolean openAbove = above != null && (above == airBlock || (lavaBlock != null && above == lavaBlock));
                    if (openBelow && openAbove) {
                        Reflect.setBlockState(primer, localX, y, localZ, airState);
                        continue;
                    }
                    // Two-high floater: solid,solid with open on both ends
                    if (openBelow && !openAbove && y + 2 <= ceilY - 2) {
                        net.minecraft.block.Block above2 = Reflect.getBlock(Reflect.getBlockState(primer, localX, y + 2, localZ));
                        boolean openAbove2 = above2 != null && (above2 == airBlock || (lavaBlock != null && above2 == lavaBlock));
                        if (openAbove2 && above != null && above != airBlock && (lavaBlock == null || above != lavaBlock)) {
                            Reflect.setBlockState(primer, localX, y, localZ, airState);
                            Reflect.setBlockState(primer, localX, y + 1, localZ, airState);
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
