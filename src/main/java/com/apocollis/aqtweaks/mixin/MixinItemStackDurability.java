package com.apocollis.aqtweaks.mixin;

import com.apocollis.aqtweaks.reskillable.PerkDurability;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemStack.class)
public abstract class MixinItemStackDurability {

    @Inject(method = "damageItem", at = @At("HEAD"), cancellable = true)
    private void aqtweaks$freeBreak(int amount, EntityLivingBase entity, CallbackInfo ci) {
        if (!(entity instanceof EntityPlayer player)) return;
        if (PerkDurability.consume(player, (ItemStack) (Object) this)) {
            ci.cancel();
        }
    }
}
