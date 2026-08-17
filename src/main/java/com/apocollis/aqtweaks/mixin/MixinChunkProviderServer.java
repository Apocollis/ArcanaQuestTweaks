package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.depths.DepthsBiomeUtil;
import com.apocollis.aqtweaks.depths.UpperTunnelNetwork;
import com.apocollis.aqtweaks.util.Reflect;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.ChunkProviderServer;
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

    @Shadow
    public WorldServer field_73251_h;

    @Inject(method = "func_185932_a", at = @At("RETURN"))
    private void onProvideChunkBreachReinforce(int chunkX, int chunkZ, CallbackInfoReturnable<Chunk> cir) {
        if (!ArcanaQuestTweaksConfig.DepthsModuleConfig.general.enableDepthsModule
                || !ArcanaQuestTweaksConfig.DepthsModuleConfig.general.enableBetterDepthsCaves) {
            return;
        }

        Chunk chunk = cir.getReturnValue();
        if (chunk == null) return;

        int minY = ArcanaQuestTweaksConfig.DepthsModuleConfig.general.minWorldY;
        if (minY >= 0) return;

        World world = this.field_73251_h != null ? this.field_73251_h : chunk.getWorld();
        long seed = world != null ? Reflect.getSeed(world) : 1337L;
        UpperTunnelNetwork.init(seed);

        if (!loggedOnce) {
            LOGGER.info("[AQ-DEPTHS] Chunk pass: tunnel-path seam reinforce Y0–4 after BC");
            loggedOnce = true;
        }

        IBlockState airState = Reflect.getAirState();
        IBlockState deepslateState = Reflect.getDeepslateState();
        net.minecraft.block.Block airBlock = Reflect.getAirBlock();
        net.minecraft.block.Block bedrockBlock = Reflect.getBedrockBlock();

        int startX = chunkX * 16;
        int startZ = chunkZ * 16;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; ++localX) {
            int worldX = startX + localX;
            for (int localZ = 0; localZ < 16; ++localZ) {
                int worldZ = startZ + localZ;
                boolean isWater = DepthsBiomeUtil.isWaterBiome(world, worldX, worldZ);
                UpperTunnelNetwork.ColumnDigCache dig = UpperTunnelNetwork.forColumn(worldX, worldZ);

                if (!isWater && dig.shouldOpenSeam()) {
                    for (int y = 0; y <= UpperTunnelNetwork.SEAM_TOP; ++y) {
                        Reflect.setPos(pos, worldX, y, worldZ);
                        IBlockState cur = Reflect.getBlockState(chunk, pos);
                        net.minecraft.block.Block b = Reflect.getBlock(cur);
                        if (cur != null && airBlock != null && b != airBlock && (bedrockBlock == null || b != bedrockBlock)) {
                            Reflect.setBlockState(chunk, pos, airState);
                        }
                        if (y <= UpperTunnelNetwork.SEAM_MAX_Y) {
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
