package com.apocollis.aqtweaks.mixin.elenaidodge;

import com.elenai.elenaidodge2.event.ClientTickEventListener;
import com.elenai.elenaidodge2.util.ClientStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Elenai counts its regeneration timer on the client and resets it to {@code regenSpeed + regenModifier}
 * after each feather. A modifier change therefore only takes effect at the next reset, so a large one (the
 * Moonfrost storm slows regeneration this way) would leave the running timer long after the modifier is
 * lowered again. When a new modifier arrives, clamp the running timer to the interval it implies, so
 * regeneration resumes as soon as the slowdown ends.
 */
@Mixin(targets = "com.elenai.elenaidodge2.network.message.CUpdateRegenMessage$Handler", remap = false)
public class MixinCUpdateRegenHandler {

    @Inject(method = "processMessage", at = @At("RETURN"), remap = false)
    private void aqtweaks$clampRegenTimer(CallbackInfo ci) {
        int interval = Math.max(1, ClientStorage.regenSpeed + ClientStorage.regenModifier);
        if (ClientTickEventListener.regen > interval) {
            ClientTickEventListener.regen = interval;
        }
    }
}
