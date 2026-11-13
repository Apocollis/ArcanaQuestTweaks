package com.apocollis.aqtweaks.mixin;

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
        ItemStack self = (ItemStack) (Object) this;
        if (com.apocollis.aqtweaks.reskillable.PerkDurability.consume(player, self)) {
            ci.cancel();
            return;
        }
        // Mana-Weave: a tagged stack lets Botania mana pay for the damage (the helper checks the tag first).
        if (com.apocollis.aqtweaks.thaumcraft.ManaRepairTag.absorbDamage(player, self, amount)) {
            ci.cancel();
        }
    }
}
