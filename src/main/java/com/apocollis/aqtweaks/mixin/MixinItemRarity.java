package com.apocollis.aqtweaks.mixin;

import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraftforge.common.IRarity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Rarity overhaul on {@code Item.getForgeRarity}. Tooltips and JEI color line 0 from this method, not from
 * {@code ItemStack.getRarity}. Custom {@link IRarity} objects are left alone.
 */
@Mixin(Item.class)
public abstract class MixinItemRarity {

    @Inject(method = "getForgeRarity", at = @At("RETURN"), cancellable = true, remap = false)
    private void aqtweaks$forgeRarity(ItemStack stack, CallbackInfoReturnable<IRarity> cir) {
        IRarity current = cir.getReturnValue();
        if (!(current instanceof EnumRarity)) return;
        EnumRarity resolved = com.apocollis.aqtweaks.rarity.RarityOverrides.resolve(stack, (EnumRarity) current);
        if (resolved != current) cir.setReturnValue(resolved);
    }
}
