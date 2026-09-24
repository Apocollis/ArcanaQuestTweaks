package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.reskillable.client.DarkVisionLight;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.EntityRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public class MixinEntityRendererLight {

    @Shadow
    private int[] lightmapColors;

    @Inject(
            method = "updateLightmap",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;updateDynamicTexture()V"
            )
    )
    private void aqtweaks$darkVision(float partialTicks, CallbackInfo ci) {
        float mix = DarkVisionLight.factor(Minecraft.getMinecraft().player);
        if (mix <= 0.0f || lightmapColors == null) return;
        for (int i = 0; i < lightmapColors.length; i++) {
            int color = lightmapColors[i];
            int red = color & 255;
            int green = (color >> 8) & 255;
            int blue = (color >> 16) & 255;
            red = red + (int) ((255 - red) * mix);
            green = green + (int) ((255 - green) * mix);
            blue = blue + (int) ((255 - blue) * mix);
            lightmapColors[i] = (color & 0xFF000000) | (blue << 16) | (green << 8) | red;
        }
    }
}
