package com.apocollis.aqtweaks.mixin.prospectus;

import cam72cam.prospectus.Prospectus;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = Prospectus.class, remap = false)
public interface InvokerProspectus {

    @Invoker("isStackWhitelisted")
    static boolean aqtweaks$whitelisted(ItemStack stack) {
        throw new AssertionError();
    }
}
