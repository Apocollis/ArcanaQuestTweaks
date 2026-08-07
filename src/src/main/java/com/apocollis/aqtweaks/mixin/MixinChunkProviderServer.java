package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.GridStructureTracker;
import com.apocollis.aqtweaks.RoguelikeDungeonSavedData;
import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Biomes;
import net.minecraft.init.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
import net.minecraftforge.common.BiomeDictionary;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ChunkProviderServer.class, remap = false)
public class MixinChunkProviderServer {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-BetterCavesUniversal");

    private static final float CAVE_THRESHOLD = 0.28f;
    private static final float CAVERN_THRESHOLD = 0.18f;

    private static FastNoise tunnelNoise1;
    private static FastNoise tunnelNoise2;
    private static FastNoise cavernNoise1;
    private static FastNoise cavernNoise2;
    private static FastNoise cavernRegionNoise;
    private static FastNoise floorIslandNoise;

    private static boolean noiseInitialized = false;
    private static boolean loggedOnce = false;

    private static synchronized void initNoiseIfNeeded(long worldSeed) {
        if (!noiseInitialized) {
            int seed1 = (int) (worldSeed & 0xFFFF);
            int seed2 = (int) ((worldSeed >> 16) & 0xFFFF);

            // Cave Tunnels (SimplexFractal)
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

            // Cavern Chambers (YUNG's ConfigFlooredCavern$Advanced defaults: freq 0.028f, gain 0.3f, octaves 1)
            cavernNoise1 = new FastNoise(seed1 + 3333);
            cavernNoise1.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            cavernNoise1.SetFrequency(0.028f);
            cavernNoise1.SetFractalOctaves(1);
            cavernNoise1.SetFractalGain(0.3f);

            cavernNoise2 = new FastNoise(seed2 + 4444);
            cavernNoise2.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            cavernNoise2.SetFrequency(0.028f);
            cavernNoise2.SetFractalOctaves(1);
            cavernNoise2.SetFractalGain(0.3f);

            // 2D Cavern Region Noise (~40-50% cavern region coverage across subterranean world)
            cavernRegionNoise = new FastNoise(seed1 + 5555);
            cavernRegionNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            cavernRegionNoise.SetFrequency(0.008f);

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
            if (world.getBiomeProvider() != null) {
                biome = world.getBiomeProvider().getBiome(new BlockPos(x, 64, z), Biomes.PLAINS);
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

    @Shadow
    public WorldServer field_73251_h; // Shadows MCP field 'world'

    @Inject(method = "func_193413_a", at = @At("HEAD"), cancellable = true)
    private void onIsInsideStructure(World worldIn, String structureName, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (structureName == null) return;
        
        String name = structureName.toLowerCase();
        if (name.startsWith("roguelikedungeon") || name.startsWith("roguelike")) {
            RoguelikeDungeonSavedData data = RoguelikeDungeonSavedData.get(worldIn);
            
            // Level-specific checks for InControl
            if (name.contains("floor_") || name.contains("floor1") || name.contains("floor2") || name.contains("floor3") || name.contains("floor4") || name.contains("floor5")) {
                int levelIndex = -2;
                if (name.contains("floor_1") || name.endsWith("floor1")) levelIndex = 0;
                else if (name.contains("floor_2") || name.endsWith("floor2")) levelIndex = 1;
                else if (name.contains("floor_3") || name.endsWith("floor3")) levelIndex = 2;
                else if (name.contains("floor_4") || name.endsWith("floor4")) levelIndex = 3;
                else if (name.contains("floor_5") || name.endsWith("floor5")) levelIndex = 4;
                
                if (levelIndex != -2 && data.getDungeonLevel(pos) == levelIndex) {
                    cir.setReturnValue(true);
                }
            } else if (name.contains("tower")) {
                if (data.getDungeonLevel(pos) == -1) {
                    cir.setReturnValue(true);
                }
            }
            // Standard check for generic "RoguelikeDungeon"
            else if ("roguelikedungeon".equals(name) || "roguelike".equals(name)) {
                if (data.isInside(pos)) {
                    cir.setReturnValue(true);
                }
            }
        }
    }

    @Inject(method = "func_180513_a", at = @At("HEAD"), cancellable = true)
    private void onGetPossibleCreatureLocations(World worldIn, String structureName, BlockPos pos, boolean findUnexplored, CallbackInfoReturnable<BlockPos> cir) {
        if ("RoguelikeDungeon".equalsIgnoreCase(structureName) || "roguelike".equalsIgnoreCase(structureName)) {
            int chunkX = pos.getX() >> 4;
            int chunkZ = pos.getZ() >> 4;
            ChunkPos nearest = GridStructureTracker.getNearestStructure(worldIn, chunkX, chunkZ);
            if (nearest != null) {
                cir.setReturnValue(new BlockPos((nearest.x << 4) + 8, 64, (nearest.z << 4) + 8));
            } else {
                cir.setReturnValue(null);
            }
        }
    }

    /**
     * Universal Better Caves -Y carving injected into ChunkProviderServer.provideChunk (func_185932_a).
     * Works on ALL world generators (Vanilla, RTG, BOP, etc.) with 0 classloader/mixin shadow crashes.
     */
    @Inject(method = "func_185932_a", at = @At("RETURN"))
    private void onProvideChunkBetterCavesUniversal(int chunkX, int chunkZ, CallbackInfoReturnable<Chunk> cir) {
        if (!ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule || !ArcanaQuestTweaksConfig.depthsModule.enableBetterCavesNegativeY) {
            return;
        }

        Chunk chunk = cir.getReturnValue();
        if (chunk == null) return;

        int minY = ArcanaQuestTweaksConfig.depthsModule.minWorldY; // -64
        if (minY >= 0) return;

        World world = this.field_73251_h != null ? this.field_73251_h : chunk.getWorld();
        long seed = world != null ? world.getSeed() : 1337L;
        initNoiseIfNeeded(seed);

        if (!loggedOnce) {
            LOGGER.info("[AQ-DEPTHS] Pre-carving native Better Caves -Y terrain in ChunkProviderServer.provideChunk (Universal RTG/Vanilla/BOP compatibility).");
            loggedOnce = true;
        }

        int caveTop = Math.min(4, ArcanaQuestTweaksConfig.depthsModule.caveTopY);
        int caveBottom = ArcanaQuestTweaksConfig.depthsModule.caveBottomY; // -60
        float caveXzComp = ArcanaQuestTweaksConfig.depthsModule.caveXzCompression;
        float caveYComp = ArcanaQuestTweaksConfig.depthsModule.caveYCompression;

        int cavernTop = Math.min(-12, ArcanaQuestTweaksConfig.depthsModule.cavernTopY);
        int cavernBottom = Math.max(minY + 4, ArcanaQuestTweaksConfig.depthsModule.cavernBottomY);
        float cavernXzComp = 0.7f;
        float cavernYComp = 1.3f;

        boolean oceanFlooding = ArcanaQuestTweaksConfig.depthsModule.enableOceanWaterCaves;

        IBlockState bedrockState = Blocks.BEDROCK.getDefaultState();
        IBlockState airState = Blocks.AIR.getDefaultState();
        IBlockState lavaState = Blocks.LAVA.getDefaultState();
        IBlockState waterState = Blocks.WATER.getDefaultState();

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

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; ++localX) {
            int worldX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int worldZ = startZ + localZ;

                // 0. Bedrock layer (Y = -64 to -61)
                for (int y = minY; y <= minY + 3; ++y) {
                    pos.setPos(worldX, y, worldZ);
                    chunk.setBlockState(pos, bedrockState);
                }

                boolean isWater = oceanFlooding && isWaterBiome(world, worldX, worldZ);
                float floorVal = floorIslandNoise.GetNoise(worldX, worldZ);
                float cavernRegionVal = cavernRegionNoise.GetNoise(worldX, worldZ);

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

                // 2. Sample Cavern Noise (YUNG's 3D dual SimplexFractal)
                float[] cavernVal1 = new float[cavernHeight];
                float[] cavernVal2 = new float[cavernHeight];

                for (int y = cavernBottom; y <= cavernTop; y += 4) {
                    int idx = y - cavernBottom;
                    if (idx >= 0 && idx < cavernHeight) {
                        float cx = worldX * cavernXzComp;
                        float cy = y * cavernYComp;
                        float cz = worldZ * cavernXzComp;
                        cavernVal1[idx] = cavernNoise1.GetNoise(cx, cy, cz);
                        cavernVal2[idx] = cavernNoise2.GetNoise(cx, cy, cz);
                    }
                }
                {
                    int lastIdx = cavernHeight - 1;
                    if (lastIdx % 4 != 0) {
                        float cx = worldX * cavernXzComp;
                        float ty = cavernTop * cavernYComp;
                        float cz = worldZ * cavernXzComp;
                        cavernVal1[lastIdx] = cavernNoise1.GetNoise(cx, ty, cz);
                        cavernVal2[lastIdx] = cavernNoise2.GetNoise(cx, ty, cz);
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

                // 3. Main Carving Loop (Y = minY+4 up to caveTop)
                for (int y = minY + 4; y <= caveTop; ++y) {
                    pos.setPos(worldX, y, worldZ);
                    IBlockState currentState = chunk.getBlockState(pos);
                    if (currentState == null || currentState.getBlock() == Blocks.AIR || currentState.getBlock() == Blocks.BEDROCK) {
                        continue;
                    }

                    boolean carve = false;

                    float ceilingTaper = 1.0f;
                    if (y >= -4) {
                        ceilingTaper = MathHelper.clamp((float)(-y) / 4.0f, 0.0f, 1.0f);
                        if (ceilingTaper <= 0.0f) continue;
                    }

                    // A. Cave Tunnels (Dual-simplex intersection forming winding tubes)
                    if (y >= caveBottom && y <= caveTop) {
                        int idx = y - caveBottom;
                        float taperedThreshold = CAVE_THRESHOLD + ((1.0f - CAVE_THRESHOLD) * (1.0f - ceilingTaper));
                        if (tunnelVal1[idx] > taperedThreshold && tunnelVal2[idx] > taperedThreshold) {
                            carve = true;
                        }
                    }

                    // B. Cavern Chambers (Region-gated dual-simplex forming open domed rooms)
                    if (!carve && cavernRegionVal > -0.05f && y >= cavernBottom && y <= cavernTop) {
                        int idx = y - cavernBottom;

                        float val1 = cavernVal1[idx];
                        float val2 = cavernVal2[idx];

                        float currentThreshold = CAVERN_THRESHOLD; // 0.18f

                        if (y >= cavernCeilingStart) {
                            float frac = (float) (cavernTop - y) / (float) Math.max(1, cavernTop - cavernCeilingStart);
                            currentThreshold += (1.0f - currentThreshold) * (1.0f - MathHelper.clamp(frac, 0.0f, 1.0f));
                        }

                        if (y < cavernFloorEnd) {
                            float frac = (float) (y - cavernBottom) / (float) Math.max(1, cavernFloorEnd - cavernBottom);
                            currentThreshold += (1.0f - currentThreshold) * (1.0f - MathHelper.clamp(frac, 0.0f, 1.0f));
                        }

                        currentThreshold += (1.0f - currentThreshold) * (1.0f - ceilingTaper);

                        // Carve open cavern room when dual noise overlap threshold is met
                        if (val1 > currentThreshold && val2 > currentThreshold) {
                            carve = true;
                        }
                    }

                    // 4. Block Placement
                    if (carve) {
                        if (isWater) {
                            if (y <= lavaLevel && floorVal > 0.12f) {
                                continue;
                            } else {
                                chunk.setBlockState(pos, waterState);
                            }
                        } else {
                            if (y <= lavaLevel) {
                                if (floorVal > 0.12f) {
                                    continue;
                                } else {
                                    chunk.setBlockState(pos, lavaState);
                                }
                            } else {
                                chunk.setBlockState(pos, airState);
                            }
                        }
                    }
                }

                // 5. Plug Y = 0 Air Plate Seam
                pos.setPos(worldX, 0, worldZ);
                IBlockState stateAt0 = chunk.getBlockState(pos);
                pos.setPos(worldX, -1, worldZ);
                IBlockState stateBelow = chunk.getBlockState(pos);
                pos.setPos(worldX, 1, worldZ);
                IBlockState stateAbove = chunk.getBlockState(pos);

                if ((stateAt0 == null || stateAt0.getBlock() == Blocks.AIR) &&
                    (stateBelow != null && stateBelow.getBlock() != Blocks.AIR && stateBelow.getBlock() != Blocks.BEDROCK) &&
                    (stateAbove != null && stateAbove.getBlock() != Blocks.AIR)) {
                    pos.setPos(worldX, 0, worldZ);
                    chunk.setBlockState(pos, stateBelow);
                }
            }
        }
    }
}

