package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.ThaumcraftPerkHooks;
import net.minecraft.entity.player.EntityPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.items.casters.CasterManager;

@Mixin(value = CasterManager.class, remap = false)
public abstract class MixinCasterManager {

    @Inject(method = "getTotalVisDiscount", at = @At("RETURN"), cancellable = true)
    private static void aqtweaks$visThrift(EntityPlayer player, CallbackInfoReturnable<Float> cir) {
        cir.setReturnValue(ThaumcraftPerkHooks.visDiscount(player, cir.getReturnValueF()));
    }
}
