package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;
import com.yungnickyoung.minecraft.bettercaves.world.MapGenBetterCaves;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.chunk.ChunkPrimer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * PASS 2: Primer-stage negative-Y assist only.
 *
 * Caps at Y <= -2 so it cannot open a 1-block air seam at Y=0.
 * Final layering (deep caverns → mid chambers → breach tunnels → pillars)
 * is owned by MixinChunkProviderServer's universal pass.
 */
@Mixin(value = MapGenBetterCaves.class, remap = false)
public abstract class MixinBetterCavesDepthsPass {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-DepthsPass");

    private static final FastNoise windingTunnelNoise = new FastNoise(1337);
    private static final FastNoise cavernChamberNoise = new FastNoise(4242);
    private static boolean noiseInitialized = false;
    private static boolean loggedOnce = false;

    /** Never carve at or above this Y — universal pass owns the Y≈0 seam + breach mouths. */
    private static final int MAX_CARVE_Y = -2;

    private static void initNoiseIfNeeded(long seed) {
        if (!noiseInitialized) {
            windingTunnelNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            windingTunnelNoise.SetFrequency(0.022f);

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

        if (worldIn.provider != null && worldIn.provider.getDimension() != 0) return;

        int minY = ArcanaQuestTweaksConfig.depthsModule.minWorldY;
        if (minY >= 0) return;

        initNoiseIfNeeded(Reflect.getSeed(worldIn));

        if (!loggedOnce) {
            LOGGER.info("[AQ-DEPTHS] Pass 2 primer assist active (maxY={}). Universal pass owns final layering.", MAX_CARVE_Y);
            loggedOnce = true;
        }

        IBlockState airState = Reflect.getAirState();
        IBlockState lavaState = Reflect.getLavaState();
        net.minecraft.block.Block airBlock = Reflect.getAirBlock();
        net.minecraft.block.Block bedrockBlock = Reflect.getBedrockBlock();

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;
        int lavaLevel = minY + 10;
        int cavernTop = ArcanaQuestTweaksConfig.depthsModule.cavernTopY;
        int midTop = ArcanaQuestTweaksConfig.depthsModule.midCaveTopY;
        int maxY = Math.min(MAX_CARVE_Y, Math.max(cavernTop, midTop));

        for (int localX = 0; localX < 16; ++localX) {
            int worldX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int worldZ = startZ + localZ;

                for (int y = minY + 1; y <= maxY; ++y) {
                    IBlockState currentState = Reflect.getBlockState(primer, localX, y, localZ);
                    net.minecraft.block.Block currentBlock = Reflect.getBlock(currentState);
                    if (currentState == null || (airBlock != null && currentBlock == airBlock) || (bedrockBlock != null && currentBlock == bedrockBlock)) {
                        continue;
                    }

                    boolean carve = false;

                    // Mid / breach assist (above deep cavern ceiling, still below Y=-2)
                    if (y >= cavernTop) {
                        float wNoise = windingTunnelNoise.GetNoise(worldX * 0.8f, y * 1.2f, worldZ * 0.8f);
                        if (wNoise > 0.08f) {
                            carve = true;
                        }
                    }

                    // Deep cavern assist
                    if (y <= cavernTop) {
                        float cNoise = cavernChamberNoise.GetNoise(worldX * 0.7f, y * 0.8f, worldZ * 0.7f);
                        if (cNoise > 0.04f) {
                            carve = true;
                        }
                    }

                    if (carve) {
                        if (y <= lavaLevel) {
                            Reflect.setBlockState(primer, localX, y, localZ, lavaState);
                        } else {
                            Reflect.setBlockState(primer, localX, y, localZ, airState);
                        }
                    }
                }
            }
        }
    }
}
