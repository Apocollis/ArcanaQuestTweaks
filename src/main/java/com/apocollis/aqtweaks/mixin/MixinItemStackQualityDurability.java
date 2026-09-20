package com.apocollis.aqtweaks.mixin;

import java.util.Random;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.item.ItemStack;

@Mixin(ItemStack.class)
public abstract class MixinItemStackQualityDurability {

    @Inject(method = "attemptDamageItem", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$qualityAfterDamage(int amount, Random rand, EntityPlayerMP player,
            CallbackInfoReturnable<Boolean> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        boolean wouldDestroy = Boolean.TRUE.equals(cir.getReturnValue());
        if (com.apocollis.aqtweaks.qualitytools.QualityDurability.afterAttemptDamage(stack, wouldDestroy, player)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "setItemDamage", at = @At("RETURN"))
    private void aqtweaks$qualityAfterSetDamage(int damage, CallbackInfo ci) {
        com.apocollis.aqtweaks.qualitytools.QualityDurability.afterSetDamage((ItemStack) (Object) this);
    }
}
