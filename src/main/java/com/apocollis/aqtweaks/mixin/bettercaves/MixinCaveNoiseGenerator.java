package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.yungnickyoung.minecraft.bettercaves.noise.FastNoise;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.ChunkPrimer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "sayys.depthsupdate.world.generation.noise.CaveNoiseGenerator", remap = false)
public abstract class MixinCaveNoiseGenerator {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-DepthsBetterCaves");

    // YUNG's RigidMulti Dual-Noise Generators for Taller & Wider Winding Tunnels
    private static final FastNoise tunnelNoise1 = new FastNoise(1337);
    private static final FastNoise tunnelNoise2 = new FastNoise(7331);
    private static final FastNoise cavernChamberNoise = new FastNoise(4242);
    private static final FastNoise floorIslandNoise = new FastNoise(9876);

    // Rounded 8-10 block wide/tall horizontal cavern connector generators
    private static final FastNoise connectorNoise1 = new FastNoise(5555);
    private static final FastNoise connectorNoise2 = new FastNoise(8888);

    private static boolean noiseInitialized = false;
    private static boolean loggedOnce = false;

    private static void initNoiseIfNeeded() {
        if (!noiseInitialized) {
            // RigidMulti Simplex Tunnels (Y = -50 to 4) - Lower Y frequency (0.018f) for tall 8-16 block vaulted tunnels
            tunnelNoise1.SetNoiseType(FastNoise.NoiseType.Simplex);
            tunnelNoise1.SetFrequency(0.018f);

            tunnelNoise2.SetNoiseType(FastNoise.NoiseType.Simplex);
            tunnelNoise2.SetFrequency(0.018f);

            // Rounded 8-10 block horizontal connectors (Y = -64 to -25)
            connectorNoise1.SetNoiseType(FastNoise.NoiseType.Simplex);
            connectorNoise1.SetFrequency(0.016f);

            connectorNoise2.SetNoiseType(FastNoise.NoiseType.Simplex);
            connectorNoise2.SetFrequency(0.016f);

            // Type 2 Cavernous Rooms Noise (Y = -64 to -25)
            cavernChamberNoise.SetNoiseType(FastNoise.NoiseType.SimplexFractal);
            cavernChamberNoise.SetFrequency(0.012f);
            cavernChamberNoise.SetFractalOctaves(2);

            // 2D Floor Island Noise for deep caverns (Y <= -55)
            floorIslandNoise.SetNoiseType(FastNoise.NoiseType.Simplex);
            floorIslandNoise.SetFrequency(0.035f);

            noiseInitialized = true;
        }
    }

    /**
     * Pre-carves negative Y terrain FIRST during Depths Update's CaveNoiseGenerator.generate pass.
     *
     * Features:
     * - Carves continuously up to Y = 4 (crossing Y = 0) so negative Y tunnels break cleanly into positive Y caves without any solid layer between them.
     * - Stretches Y noise scale (y * 0.5f) for tall 8-16 block high vaulted winding tunnels.
     * - Carves rounded 8-10 block wide/tall horizontal connector tunnels (Y = -64 to -25) linking caverns together.
     * - Uses 2D floor noise for Y <= -55 to generate solid Deepslate islands, walkways, and shorelines alongside lava lakes.
     */
    @Inject(method = "generate(IILnet/minecraft/world/chunk/ChunkPrimer;)V", at = @At("HEAD"), cancellable = true)
    private void onGenerateDepthsBetterCaves(int chunkX, int chunkZ, ChunkPrimer primer, CallbackInfo ci) {
        if (ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule && ArcanaQuestTweaksConfig.depthsModule.enableBetterCavesNegativeY) {
            if (primer == null) return;

            int minY = ArcanaQuestTweaksConfig.depthsModule.minWorldY;
            if (minY >= 0) return;

            initNoiseIfNeeded();

            if (!loggedOnce) {
                LOGGER.info("[AQ-DEPTHS] Pre-carving tall winding tunnels, 8-10 block rounded horizontal cavern connectors, and solid cavern islands.");
                loggedOnce = true;
            }

            IBlockState airState = Blocks.AIR.getDefaultState();
            IBlockState lavaState = Blocks.LAVA.getDefaultState();

            int startX = chunkX * 16;
            int startZ = chunkZ * 16;
            int lavaLevel = minY + 9; // Deep lava lakes below Y = -55

            for (int localX = 0; localX < 16; ++localX) {
                int worldX = startX + localX;
                for (int localZ = 0; localZ < 16; ++localZ) {
                    int worldZ = startZ + localZ;

                    float floorVal = floorIslandNoise.GetNoise(worldX, worldZ);

                    // Pre-carve from minY + 1 (-63) up to Y = 4 to continuously break into positive Y caves
                    for (int y = minY + 1; y <= 4; ++y) {
                        IBlockState currentState = primer.getBlockState(localX, y, localZ);
                        if (currentState == null || currentState.getBlock() == Blocks.AIR || currentState.getBlock() == Blocks.BEDROCK) {
                            continue;
                        }

                        boolean carve = false;

                        // YUNG's RigidMulti Dual-Noise Tubing (Y = -50 to 4) - Stretched Y (y * 0.5f) for tall 8-16 block tunnels
                        if (y >= -50) {
                            float n1 = tunnelNoise1.GetNoise(worldX * 0.8f, y * 0.5f, worldZ * 0.8f);
                            float n2 = tunnelNoise2.GetNoise(worldX * 0.8f, y * 0.5f, worldZ * 0.8f);
                            float threshold = 0.14f;

                            // Carve tall 8-16 block high, 6-12 block wide winding tunnels
                            if (Math.abs(n1) < threshold && Math.abs(n2) < threshold) {
                                carve = true;
                            }
                        }

                        // Rounded 8-10 block wide and tall horizontal cavern connectors (Y = -64 to -25)
                        if (!carve && y <= -25) {
                            float h1 = connectorNoise1.GetNoise(worldX * 0.8f, y * 0.8f, worldZ * 0.8f);
                            float h2 = connectorNoise2.GetNoise(worldX * 0.8f, y * 0.8f, worldZ * 0.8f);
                            if (Math.abs(h1) < 0.16f && Math.abs(h2) < 0.16f) {
                                carve = true;
                            }
                        }

                        // Type 2 Cavernous Rooms (Y = -64 to -25) - High threshold (0.55f) for distinct isolated rooms
                        if (!carve && y <= -25) {
                            float cNoise = cavernChamberNoise.GetNoise(worldX * 0.7f, y * 0.5f, worldZ * 0.7f);
                            if (cNoise > 0.55f) {
                                carve = true;
                            }
                        }

                        if (carve) {
                            // Below Y = -55: Check 2D floor noise for solid Deepslate islands / walkways
                            if (y <= lavaLevel) {
                                if (floorVal > 0.12f) {
                                    // Solid Deepslate island / walkway floor — do not carve or fill with lava!
                                    continue;
                                } else {
                                    primer.setBlockState(localX, y, localZ, lavaState);
                                }
                            } else {
                                primer.setBlockState(localX, y, localZ, airState);
                            }
                        }
                    }
                }
            }

            // Cancel Depths Update's default 1.18 spaghetti cave logic
            ci.cancel();
        }
    }
}
