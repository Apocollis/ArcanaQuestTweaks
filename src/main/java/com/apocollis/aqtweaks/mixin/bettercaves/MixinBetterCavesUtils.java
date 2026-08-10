package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.util.Reflect;
import com.yungnickyoung.minecraft.bettercaves.util.BetterCavesUtils;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
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

            net.minecraft.block.Block airBlock = Reflect.getAirBlock();
            Material airMat = Reflect.getMaterialAir();
            Material waterMat = Reflect.getMaterialWater();

            // Search DOWNWARD from Y=254 for the highest solid block that has ONLY air/water above it up to Y=255.
            for (int y = 254; y >= 1; --y) {
                IBlockState stateAt = Reflect.getBlockState(primer, x, y, z);
                if (stateAt == null) continue;

                Material matAt = Reflect.getMaterial(stateAt);
                boolean isSolid = (airBlock == null || Reflect.getBlock(stateAt) != airBlock)
                        && matAt != airMat
                        && matAt != waterMat;

                if (isSolid) {
                    // Check if all blocks above y are air or water (open sky)
                    boolean openSkyAbove = true;
                    for (int checkY = y + 1; checkY <= 255; ++checkY) {
                        IBlockState stateAbove = Reflect.getBlockState(primer, x, checkY, z);
                        if (stateAbove != null) {
                            Material matAbove = Reflect.getMaterial(stateAbove);
                            if ((airBlock != null && Reflect.getBlock(stateAbove) != airBlock)
                                    && matAbove != airMat
                                    && matAbove != waterMat) {
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
