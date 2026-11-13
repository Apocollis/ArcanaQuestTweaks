package com.apocollis.aqtweaks.mixin;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Rarity overhaul: enchantment, quality and config-override tiers on {@code ItemStack.getRarity}. */
@Mixin(ItemStack.class)
public abstract class MixinItemStackRarity {

    @Inject(method = "getRarity", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$rarity(CallbackInfoReturnable<EnumRarity> cir) {
        EnumRarity resolved = com.apocollis.aqtweaks.rarity.RarityOverrides.resolve((ItemStack) (Object) this,
                cir.getReturnValue());
        if (resolved != cir.getReturnValue()) cir.setReturnValue(resolved);
    }
}
