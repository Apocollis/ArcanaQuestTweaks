package com.apocollis.aqtweaks.mixin.animania;

import com.animania.common.handler.AddonHandler;
import net.minecraftforge.event.world.WorldEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = AddonHandler.class, remap = false)
public abstract class MixinAddonHandler {

    @Inject(method = "onWorldLoad", at = @At("HEAD"), cancellable = true)
    private static void aqtweaks$skipAdvancementReload(WorldEvent.Load event, CallbackInfo ci) {
        ci.cancel();
    }
}
