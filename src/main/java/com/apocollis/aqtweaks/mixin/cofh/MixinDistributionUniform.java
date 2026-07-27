package com.apocollis.aqtweaks.mixin.cofh;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import cofh.cofhworld.world.distribution.DistributionUniform;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = DistributionUniform.class, remap = false)
public abstract class MixinDistributionUniform {

    @Redirect(
        method = "generateFeature",
        at = @At(
            value = "INVOKE",
            target = "Ljava/lang/Math;max(II)I"
        )
    )
    private int redirectMinYClamp(int val1, int val2) {
        if (ArcanaQuestTweaksConfig.depthsModule.enableDepthsModule && ArcanaQuestTweaksConfig.depthsModule.enableCoFHNegativeY) {
            return Math.max(val1, ArcanaQuestTweaksConfig.depthsModule.minWorldY);
        }
        return Math.max(val1, val2);
    }
}
