package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Client-only: skip Overworld skybox when the camera is below Y 0 (Depths).
 */
@Mixin(RenderGlobal.class)
public class MixinRenderGlobal {

    @Inject(method = "renderSky", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$hideSkyBelowZero(float partialTicks, int pass, CallbackInfo ci) {
        if (!ArcanaQuestTweaksConfig.DepthsModuleConfig.general.enableDepthsModule) return;
        if (!ArcanaQuestTweaksConfig.DepthsModuleConfig.client.hideSkyBelowZero) return;

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.world == null || mc.world.provider == null || mc.world.provider.getDimension() != 0) return;

        Entity view = mc.getRenderViewEntity();
        if (view == null) return;
        if (view.getPositionEyes(partialTicks).y >= 0.0) return;

        ci.cancel();
    }
}
