package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.RunicShieldHud;
import mod.emt.thaumictweaker.events.RunicShieldingHudHandler;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tweaker's absorption-mode overlay always draws ten runes above the hearts. This HUD replaces it. */
@Mixin(value = RunicShieldingHudHandler.class, remap = false)
public abstract class MixinRunicShieldingHudHandler {

    @Inject(method = "RenderRunicShielding", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$skipTweakerOverlay(RenderGameOverlayEvent.Post event, CallbackInfo ci) {
        if (RunicShieldHud.skipTweakerOverlay()) {
            ci.cancel();
        }
    }
}
