package com.apocollis.aqtweaks.mixin.solcarrot;

import com.cazsius.solcarrot.client.gui.GuiFoodBook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = GuiFoodBook.class, remap = false)
public abstract class MixinGuiFoodBook {

    @Shadow
    private List<?> pages;

    @Inject(method = "initPages", at = @At("RETURN"))
    private void aqtweaks$removeConfigInfoPage(CallbackInfo ci) {
        if (this.pages != null) {
            this.pages.removeIf(page -> page != null && "com.cazsius.solcarrot.client.gui.ConfigInfoPage".equals(page.getClass().getName()));
        }
    }
}
