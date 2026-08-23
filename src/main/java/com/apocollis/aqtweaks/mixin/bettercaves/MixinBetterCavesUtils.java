package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.apocollis.aqtweaks.depths.PrimerAccess;
import com.yungnickyoung.minecraft.bettercaves.util.BetterCavesUtils;
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
     *
     * The scan itself is in {@link PrimerAccess} so the primer reads are direct calls rather than
     * reflection — this runs for all 256 columns of every generated chunk.
     */
    @Inject(method = "getSurfaceAltitudeForColumn", at = @At("HEAD"), cancellable = true)
    private static void onGetSurfaceAltitudeForColumn(ChunkPrimer primer, int x, int z, CallbackInfoReturnable<Integer> cir) {
        if (ArcanaQuestTweaksConfig.DepthsModuleConfig.general.enableDepthsModule
                && ArcanaQuestTweaksConfig.DepthsModuleConfig.compatibility.enableBetterCavesNegativeY) {
            cir.setReturnValue(PrimerAccess.openSkySurfaceY(primer, x, z));
        }
    }
}
