package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.yungnickyoung.minecraft.bettercaves.util.BetterCavesUtils;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.init.Blocks;
import net.minecraft.world.chunk.ChunkPrimer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = BetterCavesUtils.class, remap = false)
public abstract class MixinBetterCavesUtils {

    /**
     * Replaces getSurfaceAltitudeForColumn to find the TRUE ground surface where the sky meets terrain.
     *
     * A block position 'y' is the true Overworld ground surface if:
     * 1. Block at 'y' is solid terrain (not AIR and not WATER).
     * 2. ALL blocks above 'y' up to Y = 255 are AIR or WATER (the open sky).
     *
     * This prevents underground cave ceilings (which have solid stone above them) from being
     * falsely reported as the surface height.
     */
    @Inject(method = "getSurfaceAltitudeForColumn", at = @At("HEAD"), cancellable = true)
    private static void onGetSurfaceAltitudeForColumn(ChunkPrimer primer, int x, int z, CallbackInfoReturnable<Integer> cir) {
        if (ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule && ArcanaQuestTweaksConfig.depthsModule.enableBetterCavesNegativeY) {
            if (primer == null) {
                cir.setReturnValue(64);
                return;
            }

            // Search DOWNWARD from Y=254 for the highest solid block that has ONLY air/water above it up to Y=255.
            for (int y = 254; y >= 1; --y) {
                IBlockState stateAt = primer.getBlockState(x, y, z);
                if (stateAt == null) continue;

                boolean isSolid = stateAt.getBlock() != Blocks.AIR
                        && stateAt.getMaterial() != Material.AIR
                        && stateAt.getMaterial() != Material.WATER;

                if (isSolid) {
                    // Check if all blocks above y are air or water (open sky)
                    boolean openSkyAbove = true;
                    for (int checkY = y + 1; checkY <= 255; ++checkY) {
                        IBlockState stateAbove = primer.getBlockState(x, checkY, z);
                        if (stateAbove != null) {
                            if (stateAbove.getBlock() != Blocks.AIR
                                    && stateAbove.getMaterial() != Material.AIR
                                    && stateAbove.getMaterial() != Material.WATER) {
                                openSkyAbove = false;
                                break;
                            }
                        }
                    }

                    if (openSkyAbove) {
                        cir.setReturnValue(y);
                        return;
                    }
                }
            }

            // Fallback: default sea level
            cir.setReturnValue(64);
        }
    }
}
