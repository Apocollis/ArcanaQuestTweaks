package com.apocollis.aqtweaks.depths;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.block.state.IBlockState;
import net.minecraft.world.World;
import net.minecraft.world.chunk.Chunk;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Chunk-side Depths duties (Y 0-4 only):
 * <ul>
 *   <li>Reinforce breach tunnels through Y0-4 so +Y Better Caves connect (never refill land Y0).</li>
 *   <li>Water biomes: seal Y0 with Deepslate.</li>
 * </ul>
 * All -Y cavern carve/decor lives in the primer (MixinCaveNoiseGenerator); Chunk -Y writes are
 * unreliable.
 *
 * <p>Called once per newly generated chunk from the RETURN of {@code ChunkGeneratorRTG.generateChunk}
 * ({@code func_185932_a}) in {@code MixinChunkGeneratorRTG}. It used to hang off
 * {@code ChunkProviderServer}, which does not declare that method, so it never ran.
 */
public final class SeamReinforcer {

    private static final Logger LOGGER = LogManager.getLogger("AQTweaks-BetterCavesUniversal");
    private static boolean loggedOnce = false;

    private SeamReinforcer() {}

    public static void reinforce(World world, Chunk chunk, int chunkX, int chunkZ) {
        if (!ArcanaQuestTweaksConfig.DepthsModuleConfig.general.enableDepthsModule
                || !ArcanaQuestTweaksConfig.DepthsModuleConfig.general.enableBetterDepthsCaves) {
            return;
        }
        if (chunk == null || world == null || PrimerAccess.dimensionOf(world) != 0) return;

        int minY = ArcanaQuestTweaksConfig.DepthsModuleConfig.general.minWorldY;
        if (minY >= 0) return;

        long seed = ChunkAccess.getSeed(world);
        UpperTunnelNetwork.init(seed);

        if (!loggedOnce) {
            LOGGER.info("[AQ-DEPTHS] Chunk pass: tunnel-path seam reinforce Y0-4 after BC");
            loggedOnce = true;
        }

        IBlockState airState = DepthsBlocks.AIR_STATE;
        IBlockState deepslateState = DepthsBlocks.deepslateState();
        net.minecraft.block.Block airBlock = DepthsBlocks.AIR;
        net.minecraft.block.Block bedrockBlock = DepthsBlocks.BEDROCK;

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;

        for (int localX = 0; localX < 16; ++localX) {
            int worldX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int worldZ = startZ + localZ;
                boolean isWater = DepthsBiomeUtil.isWaterBiome(world, worldX, worldZ);
                UpperTunnelNetwork.ColumnDigCache dig = UpperTunnelNetwork.forColumn(worldX, worldZ);

                if (!isWater && dig.shouldOpenSeam()) {
                    for (int y = 0; y <= UpperTunnelNetwork.SEAM_TOP; ++y) {
                        IBlockState cur = ChunkAccess.getBlockState(chunk, worldX, y, worldZ);
                        net.minecraft.block.Block b = PrimerAccess.getBlock(cur);
                        if (cur != null && airBlock != null && b != airBlock && (bedrockBlock == null || b != bedrockBlock)) {
                            ChunkAccess.setBlockState(chunk, worldX, y, worldZ, airState);
                        }
                        if (y <= UpperTunnelNetwork.SEAM_MAX_Y) {
                            for (int dx = -1; dx <= 1; ++dx) {
                                for (int dz = -1; dz <= 1; ++dz) {
                                    if (dx == 0 && dz == 0) continue;
                                    int nx = worldX + dx;
                                    int nz = worldZ + dz;
                                    if ((nx >> 4) != chunkX || (nz >> 4) != chunkZ) continue;
                                    IBlockState n = ChunkAccess.getBlockState(chunk, nx, y, nz);
                                    net.minecraft.block.Block nb = PrimerAccess.getBlock(n);
                                    if (n != null && airBlock != null && nb != airBlock && (bedrockBlock == null || nb != bedrockBlock)) {
                                        ChunkAccess.setBlockState(chunk, nx, y, nz, airState);
                                    }
                                }
                            }
                        }
                    }
                }

                if (isWater && deepslateState != null) {
                    IBlockState atZero = ChunkAccess.getBlockState(chunk, worldX, 0, worldZ);
                    if (atZero != null && airBlock != null && PrimerAccess.getBlock(atZero) == airBlock) {
                        ChunkAccess.setBlockState(chunk, worldX, 0, worldZ, deepslateState);
                    }
                }
            }
        }
    }
}
