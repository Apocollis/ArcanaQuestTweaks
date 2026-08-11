package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.roguelike.GridStructureTracker;
import com.apocollis.aqtweaks.util.Reflect;
import com.apocollis.aqtweaks.roguelike.RoguelikeDungeonSavedData;
import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
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

/**
 * Chunk-side duties (Y≥0 safe):
 * - Reinforce breach tunnels through Y0–4 so +Y Better Caves connect (never refill land Y0)
 * - Water biomes: seal Y0
 *
 * All -Y cavern carve/decor lives in primer (MixinCaveNoiseGenerator) — Chunk -Y writes are unreliable.
 */
@Mixin(value = ChunkProviderServer.class, remap = false)
public class MixinChunkProviderServer {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-BetterCavesUniversal");
    private static final float BREACH_THRESHOLD = 0.075f;

    private static FastNoise breachNoise1;
    private static FastNoise breachNoise2;
    private static boolean noiseInitialized = false;
    private static boolean loggedOnce = false;

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

    @Shadow
    public WorldServer field_73251_h;

    @Inject(method = "func_193413_a", at = @At("HEAD"), cancellable = true)
    private void onIsInsideStructure(World worldIn, String structureName, BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (structureName == null) return;

        String name = structureName.toLowerCase();
        if (name.startsWith("roguelikedungeon") || name.startsWith("roguelike")) {
            RoguelikeDungeonSavedData data = RoguelikeDungeonSavedData.get(worldIn);

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
            } else if ("roguelikedungeon".equals(name) || "roguelike".equals(name)) {
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

    @Inject(method = "func_185932_a", at = @At("RETURN"))
    private void onProvideChunkBreachReinforce(int chunkX, int chunkZ, CallbackInfoReturnable<Chunk> cir) {
        if (!ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule || !ArcanaQuestTweaksConfig.depthsModule.enableBetterCavesNegativeY) {
            return;
        }

        Chunk chunk = cir.getReturnValue();
        if (chunk == null) return;

        int minY = ArcanaQuestTweaksConfig.depthsModule.minWorldY;
        if (minY >= 0) return;

        World world = this.field_73251_h != null ? this.field_73251_h : chunk.getWorld();
        long seed = world != null ? Reflect.getSeed(world) : 1337L;
        initNoiseIfNeeded(seed);

        if (!loggedOnce) {
            LOGGER.info("[AQ-DEPTHS] Chunk pass: breach reinforce Y0–4 only (no land Y0 seal; -Y decor in primer)");
            loggedOnce = true;
        }

        IBlockState airState = Reflect.getAirState();
        IBlockState deepslateState = Reflect.getDeepslateState();
        net.minecraft.block.Block airBlock = Reflect.getAirBlock();
        net.minecraft.block.Block bedrockBlock = Reflect.getBedrockBlock();

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;
        int breachTop = 4;
        int breachBottom = -25; // sample for continuity, but only WRITE y>=0 on chunk
        int height = breachTop - breachBottom + 1;

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; ++localX) {
            int worldX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int worldZ = startZ + localZ;
                boolean isWater = isWaterBiome(world, worldX, worldZ);

                float[] v1 = new float[height];
                float[] v2 = new float[height];
                sampleDual(breachNoise1, breachNoise2, worldX, worldZ, breachBottom, breachTop, 1.0f, 0.70f, v1, v2);
                applyTopDownYAdjust(v1, v2, height, BREACH_THRESHOLD);

                boolean breachMouth = false;
                if (!isWater) {
                    for (int y = 0; y <= breachTop; ++y) {
                        int idx = y - breachBottom;
                        if (idx >= 0 && idx < height && v1[idx] > BREACH_THRESHOLD && v2[idx] > BREACH_THRESHOLD) {
                            breachMouth = true;
                            Reflect.setPos(pos, worldX, y, worldZ);
                            IBlockState cur = Reflect.getBlockState(chunk, pos);
                            net.minecraft.block.Block b = Reflect.getBlock(cur);
                            if (cur != null && airBlock != null && b != airBlock && (bedrockBlock == null || b != bedrockBlock)) {
                                Reflect.setBlockState(chunk, pos, airState);
                            }
                            // Widen mouth at Y0–2
                            if (y <= 2) {
                                for (int dx = -1; dx <= 1; ++dx) {
                                    for (int dz = -1; dz <= 1; ++dz) {
                                        if (dx == 0 && dz == 0) continue;
                                        Reflect.setPos(pos, worldX + dx, y, worldZ + dz);
                                        // only same chunk
                                        if (((worldX + dx) >> 4) != chunkX || ((worldZ + dz) >> 4) != chunkZ) continue;
                                        IBlockState n = Reflect.getBlockState(chunk, pos);
                                        net.minecraft.block.Block nb = Reflect.getBlock(n);
                                        if (n != null && airBlock != null && nb != airBlock && (bedrockBlock == null || nb != bedrockBlock)) {
                                            Reflect.setBlockState(chunk, pos, airState);
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Also open Y0 if tunnel is strong just below (sampled) even if Y0 sample weak
                    int idxM1 = -1 - breachBottom;
                    if (!breachMouth && idxM1 >= 0 && idxM1 < height
                            && v1[idxM1] > BREACH_THRESHOLD && v2[idxM1] > BREACH_THRESHOLD) {
                        Reflect.setPos(pos, worldX, 0, worldZ);
                        IBlockState cur = Reflect.getBlockState(chunk, pos);
                        net.minecraft.block.Block b = Reflect.getBlock(cur);
                        if (cur != null && airBlock != null && b != airBlock && (bedrockBlock == null || b != bedrockBlock)) {
                            Reflect.setBlockState(chunk, pos, airState);
                        }
                        breachMouth = true;
                    }
                }

                // NEVER refill land Y0 — that was sealing breaches into +Y caves
                if (isWater && deepslateState != null) {
                    Reflect.setPos(pos, worldX, 0, worldZ);
                    IBlockState atZero = Reflect.getBlockState(chunk, pos);
                    if (atZero != null && airBlock != null && Reflect.getBlock(atZero) == airBlock) {
                        Reflect.setBlockState(chunk, pos, deepslateState);
                    }
                }
            }
        }
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
        int h = yTop - yBottom + 1;
        if (h <= 0) return;
        for (int y = yBottom; y <= yTop; y += 4) {
            int idx = y - yBottom;
            if (idx >= 0 && idx < h) {
                out1[idx] = n1.GetNoise(worldX * xzComp, y * yComp, worldZ * xzComp);
                out2[idx] = n2.GetNoise(worldX * xzComp, y * yComp, worldZ * xzComp);
            }
        }
        int last = h - 1;
        if (last % 4 != 0) {
            out1[last] = n1.GetNoise(worldX * xzComp, yTop * yComp, worldZ * xzComp);
            out2[last] = n2.GetNoise(worldX * xzComp, yTop * yComp, worldZ * xzComp);
        }
        for (int sub = 0; sub < h - 1; sub += 4) {
            int end = Math.min(sub + 4, h - 1);
            float s1 = out1[sub], e1 = out1[end];
            float s2 = out2[sub], e2 = out2[end];
            int span = end - sub;
            for (int i = 1; i < span; ++i) {
                float t = (float) i / (float) span;
                out1[sub + i] = s1 * (1.0f - t) + e1 * t;
                out2[sub + i] = s2 * (1.0f - t) + e2 * t;
            }
        }
    }
}
