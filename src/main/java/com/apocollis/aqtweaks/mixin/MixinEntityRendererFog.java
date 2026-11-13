package com.apocollis.aqtweaks.mixin;

import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Applies the Nether ash fog after every {@code RenderFogEvent} handler has run (Dynamic Surroundings, PotionCore,
 * Lycanites and the others all set fog from that event), so none of them can override it. The handler itself only
 * acts in the Nether while the camera is inside an ash cloud. The Aurorian Moonfrost storm has no fog (the view is
 * limited by snow particles), so nothing Aurorian is applied here.
 */
@Mixin(EntityRenderer.class)
public abstract class MixinEntityRendererFog {

    @Shadow
    private float farPlaneDistance;

    @Inject(method = "setupFog", at = @At("RETURN"))
    private void aqtweaks$ashFog(int startCoords, float partialTicks, CallbackInfo ci) {
        com.apocollis.aqtweaks.client.nether.AshFogHandler.applyAfterSetupFog(startCoords, partialTicks, this.farPlaneDistance);
    }
}
