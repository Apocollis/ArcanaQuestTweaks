package com.apocollis.aqtweaks.mixin.atum;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraftforge.client.event.EntityViewRenderEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to cancel Atum's built-in 2D full-screen sandstorm overlay and blinding EXP fog.
 * Allows AQTweaks' 3D volumetric particles and clean linear fog to take over.
 */
@Mixin(targets = "com.teammetallurgy.atum.utils.event.ClientEvents", remap = false)
public class MixinAtumClientEvents {

    @Inject(method = "renderSand", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onRenderSand(float partialTicks, int[] layers, CallbackInfo ci) {
        if (ArcanaQuestTweaksConfig.ClientModuleConfig.atumFog.disableUndergroundFog) {
            ci.cancel();
        }
    }

    @Inject(method = "renderFog", at = @At("HEAD"), cancellable = true, remap = false)
    private static void onRenderFog(EntityViewRenderEvent.RenderFogEvent event, CallbackInfo ci) {
        if (ArcanaQuestTweaksConfig.ClientModuleConfig.atumFog.disableUndergroundFog) {
            ci.cancel();
        }
    }
}
