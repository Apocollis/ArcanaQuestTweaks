package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.depths.BreachTunnelNoise;
import com.apocollis.aqtweaks.depths.UpperTunnelNetwork;
import com.apocollis.aqtweaks.util.Reflect;
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
 * After native Better Caves: open tunnel mouths at Y0 into +Y BC caves (does not cancel BC).
 * Uses the same {@link UpperTunnelNetwork} path as upper deep caves.
 */
@Mixin(value = MapGenBetterCaves.class, remap = false)
public abstract class MixinBetterCavesDepthsPass {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-BreachPass");
    private static boolean loggedOnce = false;

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

        UpperTunnelNetwork.init(Reflect.getSeed(worldIn));

        if (!loggedOnce) {
            LOGGER.info("[AQ-DEPTHS] BC companion: tunnel-path mouths into +Y after Better Caves");
            loggedOnce = true;
        }

        IBlockState airState = Reflect.getAirState();
        net.minecraft.block.Block airBlock = Reflect.getAirBlock();
        net.minecraft.block.Block bedrockBlock = Reflect.getBedrockBlock();

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;
        boolean[][] seamCore = new boolean[16][16];

        for (int localX = 0; localX < 16; ++localX) {
            int worldX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int worldZ = startZ + localZ;
                if (isWaterBiome(worldIn, worldX, worldZ)) continue;

                if (!UpperTunnelNetwork.shouldOpenSeam(worldX, worldZ)) continue;

                seamCore[localX][localZ] = true;
                for (int y = BreachTunnelNoise.SEAM_MIN_Y; y <= BreachTunnelNoise.TOP; ++y) {
                    tryCarve(primer, localX, localZ, y, airState, airBlock, bedrockBlock);
                }
            }
        }

        for (int localX = 0; localX < 16; ++localX) {
            for (int localZ = 0; localZ < 16; ++localZ) {
                if (!seamCore[localX][localZ]) continue;
                for (int y = BreachTunnelNoise.SEAM_MIN_Y; y <= BreachTunnelNoise.SEAM_MAX_Y; ++y) {
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
