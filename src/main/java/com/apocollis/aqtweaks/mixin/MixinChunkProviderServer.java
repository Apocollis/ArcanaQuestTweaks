package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.roguelike.GridStructureTracker;
import com.apocollis.aqtweaks.util.Reflect;
import com.apocollis.aqtweaks.roguelike.RoguelikeDungeonSavedData;
import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;
import net.minecraft.block.state.IBlockState;
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
    private static final float BREACH_SEAM_THRESHOLD = 0.38f; // Stricter through Y≈0 so we don't open a sheet
    private static final float MID_CAVE_THRESHOLD = 0.32f;

    private static FastNoise tunnelNoise1;
    private static FastNoise tunnelNoise2;
    private static FastNoise cavernNoise1;
    private static FastNoise cavernNoise2;
    private static FastNoise midCaveNoise1;
    private static FastNoise midCaveNoise2;
    private static FastNoise cavernRegionNoise;
    private static FastNoise pillarSpawnNoise;
    private static FastNoise pillarJitterNoise;
    private static FastNoise floorIslandNoise;

    private static boolean noiseInitialized = false;
    private static boolean loggedOnce = false;

    private static synchronized void initNoiseIfNeeded(long worldSeed) {
        if (!noiseInitialized) {
            int seed1 = (int) (worldSeed & 0xFFFF);
            int seed2 = (int) ((worldSeed >> 16) & 0xFFFF);

            // Breach tunnels (SimplexFractal dual intersection)
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

            // Deep caverns (lower frequency → wide chambers)
            cavernNoise1 = new FastNoise(seed1 + 3333);
            cavernNoise1.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            cavernNoise1.SetFrequency(0.022f);
            cavernNoise1.SetFractalOctaves(1);
            cavernNoise1.SetFractalGain(0.3f);

            cavernNoise2 = new FastNoise(seed2 + 4444);
            cavernNoise2.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            cavernNoise2.SetFrequency(0.022f);
            cavernNoise2.SetFractalOctaves(1);
            cavernNoise2.SetFractalGain(0.3f);

            // Mid chambers (-25 → -5): higher frequency → smaller rooms
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

            cavernRegionNoise = new FastNoise(seed1 + 7777);
            cavernRegionNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            cavernRegionNoise.SetFrequency(0.008f);

            // Cell-based pillar spawn + jitter for rounded off-grid centers
            pillarSpawnNoise = new FastNoise(seed1 + 8888);
            pillarSpawnNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            pillarSpawnNoise.SetFrequency(1.0f);

            pillarJitterNoise = new FastNoise(seed2 + 9999);
            pillarJitterNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            pillarJitterNoise.SetFrequency(1.0f);

            floorIslandNoise = new FastNoise(seed2 + 9876);
            floorIslandNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            floorIslandNoise.SetFrequency(0.035f);

            noiseInitialized = true;
        }
    }

    /**
     * Rounded pillar radius at this column, or 0 if outside any pillar.
     * Cell grid + jittered centers → ~6–8 block diameter cylinders that taper with heightFrac.
     */
    private static float pillarRadiusAt(int worldX, int worldZ, float heightFrac, float baseRadius, int spacing, float spawnThreshold) {
        if (spacing < 1) return 0.0f;
        int cellX = Math.floorDiv(worldX, spacing);
        int cellZ = Math.floorDiv(worldZ, spacing);
        float best = 0.0f;

        // Check this cell and neighbors so pillars near cell edges stay continuous
        for (int dx = -1; dx <= 1; ++dx) {
            for (int dz = -1; dz <= 1; ++dz) {
                int cx = cellX + dx;
                int cz = cellZ + dz;
                float spawn = pillarSpawnNoise.GetNoise(cx * 17.3f, cz * 31.7f);
                if (spawn < spawnThreshold) continue;

                float jx = pillarJitterNoise.GetNoise(cx * 13.1f, cz * 19.7f);
                float jz = pillarJitterNoise.GetNoise(cx * 23.9f + 50.0f, cz * 11.3f);
                float centerX = cx * spacing + spacing * 0.5f + jx * (spacing * 0.28f);
                float centerZ = cz * spacing + spacing * 0.5f + jz * (spacing * 0.28f);
                float dist = MathHelper.sqrt((worldX - centerX) * (worldX - centerX) + (worldZ - centerZ) * (worldZ - centerZ));

                // Broad base, taper toward ceiling (heightFrac 0 at floor → 1 at cavern top)
                float radius = baseRadius * (1.0f - 0.55f * heightFrac);
                float edge = 0.85f; // soft round edge
                if (dist <= radius) {
                    float strength = 1.0f - (dist / Math.max(0.001f, radius));
                    if (strength > edge) strength = 1.0f;
                    else strength = strength / edge;
                    if (strength > best) best = strength;
                }
            }
        }
        return best;
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
     * Layer map:
     * - Deep caverns: cavernBottom → cavernTop (~-25)
     * - Mid chambers: midCaveBottom (~-25) → midCaveTop (~-5)
     * - Breach tunnels: caveBottom (~-25) → caveTop (~4), sparse through Y≈0
     * - Rounded deepslate pillars from floor up into deep cavern ceilings
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
        long seed = world != null ? Reflect.getSeed(world) : 1337L;
        initNoiseIfNeeded(seed);

        if (!loggedOnce) {
            LOGGER.info("[AQ-DEPTHS] Universal -Y carve: deep caverns→{}, mid→{}, breach→{}, pillars r={}",
                    ArcanaQuestTweaksConfig.depthsModule.cavernTopY,
                    ArcanaQuestTweaksConfig.depthsModule.midCaveTopY,
                    ArcanaQuestTweaksConfig.depthsModule.caveTopY,
                    ArcanaQuestTweaksConfig.depthsModule.pillarRadius);
            loggedOnce = true;
        }

        int caveTop = Math.min(4, ArcanaQuestTweaksConfig.depthsModule.caveTopY);
        int caveBottom = ArcanaQuestTweaksConfig.depthsModule.caveBottomY;
        // Migrate pre-1.6 defaults that left caverns at -12 and tunnels to -60
        if (caveBottom < -40) {
            caveBottom = -25;
        }
        float caveXzComp = ArcanaQuestTweaksConfig.depthsModule.caveXzCompression;
        float caveYComp = ArcanaQuestTweaksConfig.depthsModule.caveYCompression;

        int cavernTop = ArcanaQuestTweaksConfig.depthsModule.cavernTopY;
        if (cavernTop > -20) {
            cavernTop = -25;
        }
        int cavernBottom = Math.max(minY + 4, ArcanaQuestTweaksConfig.depthsModule.cavernBottomY);
        float cavernXzComp = ArcanaQuestTweaksConfig.depthsModule.cavernXzCompression;
        float cavernYComp = ArcanaQuestTweaksConfig.depthsModule.cavernYCompression;

        int midTop = ArcanaQuestTweaksConfig.depthsModule.midCaveTopY;
        int midBottom = ArcanaQuestTweaksConfig.depthsModule.midCaveBottomY;

        float pillarRadius = ArcanaQuestTweaksConfig.depthsModule.pillarRadius;
        int pillarSpacing = ArcanaQuestTweaksConfig.depthsModule.pillarSpacing;
        float pillarSpawnThreshold = ArcanaQuestTweaksConfig.depthsModule.pillarSpawnThreshold;

        IBlockState bedrockState = Reflect.getBedrockState();
        IBlockState airState = Reflect.getAirState();
        IBlockState lavaState = Reflect.getLavaState();
        IBlockState deepslateState = Reflect.getDeepslateState();
        net.minecraft.block.Block airBlock = Reflect.getAirBlock();
        net.minecraft.block.Block bedrockBlock = Reflect.getBedrockBlock();

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;
        int lavaLevel = minY + 9; // Y = -55

        int caveHeight = Math.max(1, caveTop - caveBottom + 1);
        int cavernHeight = Math.max(1, cavernTop - cavernBottom + 1);
        int midHeight = Math.max(1, midTop - midBottom + 1);

        int cavernCeilingStart = cavernTop - 8;
        int cavernFloorEnd;
        if (cavernBottom < lavaLevel) {
            cavernFloorEnd = lavaLevel + 8;
        } else {
            cavernFloorEnd = cavernBottom + 7;
        }

        int pillarBaseY = minY + 4; // ~-60
        int pillarTopY = cavernTop; // taper into deep cavern ceiling

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; ++localX) {
            int worldX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int worldZ = startZ + localZ;

                // 0. Bedrock layer (Y = -64 to -61)
                for (int y = minY; y <= minY + 3; ++y) {
                    Reflect.setPos(pos, worldX, y, worldZ);
                    Reflect.setBlockState(chunk, pos, bedrockState);
                }

                boolean isWater = isWaterBiome(world, worldX, worldZ);
                float floorVal = floorIslandNoise.GetNoise(worldX, worldZ);
                float cavernRegionVal = cavernRegionNoise.GetNoise(worldX, worldZ);

                // 1. Sample breach tunnel noise (caveBottom → caveTop)
                float[] tunnelVal1 = new float[caveHeight];
                float[] tunnelVal2 = new float[caveHeight];
                sampleDualNoise(tunnelNoise1, tunnelNoise2, worldX, worldZ, caveBottom, caveTop,
                        caveXzComp, caveYComp, tunnelVal1, tunnelVal2);

                // 2. Sample deep cavern noise
                float[] cavernVal1 = new float[cavernHeight];
                float[] cavernVal2 = new float[cavernHeight];
                sampleDualNoise(cavernNoise1, cavernNoise2, worldX, worldZ, cavernBottom, cavernTop,
                        cavernXzComp, cavernYComp, cavernVal1, cavernVal2);

                // 3. Sample mid chamber noise
                float[] midVal1 = new float[midHeight];
                float[] midVal2 = new float[midHeight];
                sampleDualNoise(midCaveNoise1, midCaveNoise2, worldX, worldZ, midBottom, midTop,
                        1.0f, 1.4f, midVal1, midVal2);

                // Breach flag at Y=0 for seam sealing
                boolean breachAtZero = false;
                if (!isWater && caveBottom <= 0 && 0 <= caveTop) {
                    int idx0 = 0 - caveBottom;
                    if (idx0 >= 0 && idx0 < caveHeight
                            && tunnelVal1[idx0] > BREACH_SEAM_THRESHOLD
                            && tunnelVal2[idx0] > BREACH_SEAM_THRESHOLD) {
                        breachAtZero = true;
                    }
                }

                int effectiveMaxY = isWater ? -20 : caveTop;

                // 4. Main carving loop
                for (int y = minY + 4; y <= effectiveMaxY; ++y) {
                    Reflect.setPos(pos, worldX, y, worldZ);
                    IBlockState currentState = Reflect.getBlockState(chunk, pos);
                    net.minecraft.block.Block currentBlock = Reflect.getBlock(currentState);
                    boolean isAir = airBlock != null && currentBlock == airBlock;
                    if (currentState == null || (bedrockBlock != null && currentBlock == bedrockBlock)) {
                        continue;
                    }

                    boolean carve = false;

                    // A. Breach tunnels (-25 → 4). Near Y=0 use stricter threshold (no sheet gap).
                    if (!isWater && y >= caveBottom && y <= caveTop) {
                        int idx = y - caveBottom;
                        float threshold = CAVE_THRESHOLD;
                        if (y >= -2 && y <= 1) {
                            threshold = BREACH_SEAM_THRESHOLD;
                        }
                        float tunnelTaper = 1.0f;
                        if (y >= 0) {
                            tunnelTaper = MathHelper.clamp((float) (caveTop - y) / 4.0f, 0.0f, 1.0f);
                        }
                        float taperedThreshold = threshold + ((1.0f - threshold) * (1.0f - tunnelTaper));
                        if (idx >= 0 && idx < caveHeight
                                && tunnelVal1[idx] > taperedThreshold
                                && tunnelVal2[idx] > taperedThreshold) {
                            carve = true;
                        }
                    }

                    // B. Deep caverns (bottom → ~-25)
                    if (!carve && cavernRegionVal > -0.05f && y >= cavernBottom && y <= cavernTop) {
                        int idx = y - cavernBottom;
                        if (idx >= 0 && idx < cavernHeight) {
                            float product = cavernVal1[idx] * cavernVal2[idx];
                            float currentThreshold = 0.20f;

                            if (y >= cavernCeilingStart) {
                                float frac = (float) (cavernTop - y) / (float) Math.max(1, cavernTop - cavernCeilingStart);
                                currentThreshold += (1.0f - currentThreshold) * (1.0f - MathHelper.clamp(frac, 0.0f, 1.0f));
                            }
                            if (y < cavernFloorEnd) {
                                float frac = (float) (y - cavernBottom) / (float) Math.max(1, cavernFloorEnd - cavernBottom);
                                currentThreshold += (1.0f - currentThreshold) * (1.0f - MathHelper.clamp(frac, 0.0f, 1.0f));
                            }
                            if (product > currentThreshold) {
                                carve = true;
                            }
                        }
                    }

                    // C. Mid chambers (~-25 → ~-5) — smaller product rooms
                    if (!carve && y >= midBottom && y <= midTop) {
                        int idx = y - midBottom;
                        if (idx >= 0 && idx < midHeight) {
                            float product = midVal1[idx] * midVal2[idx];
                            float currentThreshold = MID_CAVE_THRESHOLD;
                            // Soft ceiling into solid stone under Y≈-5
                            if (y >= midTop - 4) {
                                float frac = (float) (midTop - y) / 4.0f;
                                currentThreshold += (1.0f - currentThreshold) * (1.0f - MathHelper.clamp(frac, 0.0f, 1.0f));
                            }
                            if (product > currentThreshold) {
                                carve = true;
                            }
                        }
                    }

                    if (carve && !isAir) {
                        if (y <= lavaLevel) {
                            if (floorVal > 0.12f) {
                                // keep solid floor islands
                            } else {
                                Reflect.setBlockState(chunk, pos, lavaState);
                            }
                        } else {
                            Reflect.setBlockState(chunk, pos, airState);
                            isAir = true;
                        }
                    }

                    // D. Rounded deepslate pillars (floor → deep cavern ceiling)
                    if (deepslateState != null && y >= pillarBaseY && y <= pillarTopY && cavernRegionVal > -0.10f) {
                        float heightFrac = MathHelper.clamp(
                                (float) (y - pillarBaseY) / (float) Math.max(1, pillarTopY - pillarBaseY),
                                0.0f, 1.0f);
                        float strength = pillarRadiusAt(worldX, worldZ, heightFrac, pillarRadius, pillarSpacing, pillarSpawnThreshold);
                        if (strength > 0.35f) {
                            Reflect.setBlockState(chunk, pos, deepslateState);
                        }
                    }
                }

                // 5. Seal Y=0 seam: refill stray air left by earlier passes unless this is a breach mouth
                if (!isWater && deepslateState != null && !breachAtZero) {
                    Reflect.setPos(pos, worldX, 0, worldZ);
                    IBlockState atZero = Reflect.getBlockState(chunk, pos);
                    if (atZero != null && airBlock != null && Reflect.getBlock(atZero) == airBlock) {
                        Reflect.setBlockState(chunk, pos, deepslateState);
                    }
                }
            }
        }
    }

    private static void sampleDualNoise(FastNoise n1, FastNoise n2, int worldX, int worldZ,
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

