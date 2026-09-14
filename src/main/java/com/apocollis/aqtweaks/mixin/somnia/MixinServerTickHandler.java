package com.apocollis.aqtweaks.mixin.somnia;

import com.apocollis.aqtweaks.somnia.SomniaSleepHandler;
import com.kingrunes.somnia.server.ServerTickHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ServerTickHandler.class, remap = false)
public abstract class MixinServerTickHandler {

    @Inject(method = "doMultipliedTicking", at = @At("HEAD"), cancellable = true)
    private void aqtweaks_onDoMultipliedTicking(CallbackInfo ci) {
        if (SomniaSleepHandler.handleMultipliedTicking((ServerTickHandler) (Object) this)) {
            ci.cancel();
        }
    }
}
