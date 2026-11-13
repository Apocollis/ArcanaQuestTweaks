package com.apocollis.aqtweaks.mixin.aurorian;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import com.shiroroku.theaurorian.World.AurorianWorldProvider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Aurorian has no day/night cycle, but its provider drifts the celestial angle and cycles the moon
 * phase with world time. The Darkness mod scales the lightmap by those, so the screen brightness swings.
 * Pin both to configured constants for a permanent moonlit look. Gameplay sky light is handled
 * separately by {@code MixinWorldFixedSkylight}.
 */
@Mixin(value = AurorianWorldProvider.class, remap = false)
public abstract class MixinAurorianWorldProvider {

    /** {@code WorldProvider.getMoonPhase(long)}. */
    @Inject(method = {"func_76559_b", "getMoonPhase"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void aqtweaks$fixedMoonPhase(long worldTime, CallbackInfoReturnable<Integer> cir) {
        if (ArcanaQuestTweaksConfig.AurorianModuleConfig.enableFixedLighting) {
            cir.setReturnValue(Math.floorMod(ArcanaQuestTweaksConfig.AurorianModuleConfig.moonPhase, 8));
        }
    }

    /** {@code WorldProvider.calculateCelestialAngle(long, float)}. */
    @Inject(method = {"func_76563_a", "calculateCelestialAngle"}, at = @At("HEAD"), cancellable = true, remap = false)
    private void aqtweaks$fixedCelestialAngle(long worldTime, float partialTicks, CallbackInfoReturnable<Float> cir) {
        if (ArcanaQuestTweaksConfig.AurorianModuleConfig.enableFixedLighting) {
            cir.setReturnValue((float) ArcanaQuestTweaksConfig.AurorianModuleConfig.celestialAngle);
        }
    }
}
