package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;
import com.yungnickyoung.minecraft.bettercaves.world.MapGenBetterCaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PASS 2: Dedicated Negative Y Carver Pass.
 *
 * Targets MapGenBetterCaves (remap = false since it's a mod class) to inject
 * at the RETURN of func_186125_a. This guarantees execution after Pass 1
 * completes its native positive Y carving.
 *
 * Layer Distribution:
 * - Type 1 Winding Caves: Y = -40 to 0 (connects into positive Y caves at Y = 0)
 * - Type 2 Cavernous Chambers: Y = -64 to -20 (wide open underground chambers)
 * - Floored & Liquid Caverns: Y = -64 to -20 (lava lakes below Y = -54)
 */
@Mixin(value = MapGenBetterCaves.class, remap = false)
public abstract class MixinBetterCavesDepthsPass {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-DepthsPass");

    private static final FastNoise windingTunnelNoise = new FastNoise(1337);
    private static final FastNoise cavernChamberNoise = new FastNoise(4242);
    private static boolean noiseInitialized = false;
    private static boolean loggedOnce = false;

    private static void initNoiseIfNeeded(long seed) {
        if (!noiseInitialized) {
            // Type 1 Winding Caves Noise (Y = -40 to 0)
            windingTunnelNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            windingTunnelNoise.SetFrequency(0.022f);

            // Type 2 Cavernous Chambers Noise (Y = -64 to -20)
            cavernChamberNoise.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            cavernChamberNoise.SetFrequency(0.014f);
            cavernChamberNoise.SetFractalOctaves(2);

            noiseInitialized = true;
        }
        windingTunnelNoise.SetSeed((int) seed);
        cavernChamberNoise.SetSeed((int) seed + 99);
    }

    @Inject(method = "func_186125_a", at = @At("RETURN"))
    private void onGenerateDepthsPass(World worldIn, int chunkX, int chunkZ, ChunkPrimer primer, CallbackInfo ci) {
        if (!ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule || !ArcanaQuestTweaksConfig.depthsModule.enableBetterCavesNegativeY) {
            return;
        }
        if (worldIn == null || primer == null) return;

        // Only run on Overworld (Dim 0)
        if (worldIn.provider != null && worldIn.provider.getDimension() != 0) return;

        int minY = ArcanaQuestTweaksConfig.depthsModule.minWorldY;
        if (minY >= 0) return;

        initNoiseIfNeeded(Reflect.getSeed(worldIn));

        if (!loggedOnce) {
            LOGGER.info("[AQ-DEPTHS] Pass 2 negative Y carver active. minY={}, chunk=[{}, {}]", minY, chunkX, chunkZ);
            loggedOnce = true;
        }

        IBlockState airState = Reflect.getAirState();
        IBlockState lavaState = Reflect.getLavaState();
        net.minecraft.block.Block airBlock = Reflect.getAirBlock();
        net.minecraft.block.Block bedrockBlock = Reflect.getBedrockBlock();

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;
        int lavaLevel = minY + 10; // Lava lakes below Y = -54
        int carvedCount = 0;

        for (int localX = 0; localX < 16; ++localX) {
            int worldX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int worldZ = startZ + localZ;

                // Carve from minY + 1 (-63) up to Y = 0
                for (int y = minY + 1; y <= 0; ++y) {
                    IBlockState currentState = Reflect.getBlockState(primer, localX, y, localZ);
                    net.minecraft.block.Block currentBlock = Reflect.getBlock(currentState);
                    if (currentState == null || (airBlock != null && currentBlock == airBlock) || (bedrockBlock != null && currentBlock == bedrockBlock)) {
                        continue;
                    }

                    boolean carve = false;

                    // Type 1 Winding Caves Layer (Y = -40 to 0)
                    if (y >= -40) {
                        float wNoise = windingTunnelNoise.GetNoise(worldX * 0.8f, y * 1.2f, worldZ * 0.8f);
                        if (wNoise > 0.02f) {
                            carve = true;
                        }
                    }

                    // Type 2 Cavernous Chambers & Floored Caverns Layer (Y = -64 to -20)
                    if (y <= -20) {
                        float cNoise = cavernChamberNoise.GetNoise(worldX * 0.7f, y * 0.8f, worldZ * 0.7f);
                        if (cNoise > 0.04f) {
                            carve = true;
                        }
                    }

                    if (carve) {
                        carvedCount++;
                        if (y <= lavaLevel) {
                            Reflect.setBlockState(primer, localX, y, localZ, lavaState);
                        } else {
                            Reflect.setBlockState(primer, localX, y, localZ, airState);
                        }
                    }
                }
            }
        }

        if (carvedCount > 0 && !loggedOnce) {
            LOGGER.info("[AQ-DEPTHS] Carved {} blocks in chunk [{}, {}]", carvedCount, chunkX, chunkZ);
        }
    }
}
