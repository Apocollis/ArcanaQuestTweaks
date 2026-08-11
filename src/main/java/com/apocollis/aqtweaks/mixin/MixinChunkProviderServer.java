package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.depths.BreachTunnelNoise;
import com.apocollis.aqtweaks.roguelike.GridStructureTracker;
import com.apocollis.aqtweaks.util.Reflect;
import com.apocollis.aqtweaks.roguelike.RoguelikeDungeonSavedData;
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
    private static boolean loggedOnce = false;

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
        BreachTunnelNoise.init(seed);

        if (!loggedOnce) {
            LOGGER.info("[AQ-DEPTHS] Chunk pass: breach seam reinforce Y0–4 (narrow tubes + forced mouths)");
            loggedOnce = true;
        }

        IBlockState airState = Reflect.getAirState();
        IBlockState deepslateState = Reflect.getDeepslateState();
        net.minecraft.block.Block airBlock = Reflect.getAirBlock();
        net.minecraft.block.Block bedrockBlock = Reflect.getBedrockBlock();

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;
        int height = BreachTunnelNoise.height();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; ++localX) {
            int worldX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int worldZ = startZ + localZ;
                boolean isWater = isWaterBiome(world, worldX, worldZ);

                float[] v1 = new float[height];
                float[] v2 = new float[height];
                BreachTunnelNoise.sampleColumn(worldX, worldZ, v1, v2);
                boolean forceSeam = !isWater && BreachTunnelNoise.shouldOpenSeam(v1, v2);

                if (!isWater) {
                    for (int y = 0; y <= BreachTunnelNoise.TOP; ++y) {
                        if (!BreachTunnelNoise.shouldCarve(y, v1, v2, forceSeam)) continue;

                        Reflect.setPos(pos, worldX, y, worldZ);
                        IBlockState cur = Reflect.getBlockState(chunk, pos);
                        net.minecraft.block.Block b = Reflect.getBlock(cur);
                        if (cur != null && airBlock != null && b != airBlock && (bedrockBlock == null || b != bedrockBlock)) {
                            Reflect.setBlockState(chunk, pos, airState);
                        }

                        // Widen mouth at seam Y0–2
                        if (y <= BreachTunnelNoise.SEAM_MAX_Y) {
                            for (int dx = -1; dx <= 1; ++dx) {
                                for (int dz = -1; dz <= 1; ++dz) {
                                    if (dx == 0 && dz == 0) continue;
                                    int nx = worldX + dx;
                                    int nz = worldZ + dz;
                                    if ((nx >> 4) != chunkX || (nz >> 4) != chunkZ) continue;
                                    Reflect.setPos(pos, nx, y, nz);
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
}
