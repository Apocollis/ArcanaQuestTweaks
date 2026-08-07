package com.apocollis.aqtweaks.mixin.bettercaves;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.yungnickyoung.minecraft.bettercaves.world.carver.cavern.CavernCarver;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CavernCarver.class, remap = false)
public abstract class MixinCavernCarver {

    @Shadow
    private int bottomY;

    /**
     * Extends Yung's native CavernCarver bottomY bound down to minWorldY (-64).
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInitCavernCarver(CallbackInfo ci) {
        if (ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule && ArcanaQuestTweaksConfig.depthsModule.enableBetterCavesNegativeY) {
            int minY = ArcanaQuestTweaksConfig.depthsModule.minWorldY;
            if (minY < 0) {
                this.bottomY = minY + 1; // Extend bottomY down to -63
            }
        }
    }
}
