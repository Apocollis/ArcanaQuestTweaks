package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.ThaumcraftPerkHooks;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.items.casters.ItemCaster;

@Mixin(value = ItemCaster.class, remap = false)
public abstract class MixinItemCaster {

    @ModifyVariable(method = "consumeVis", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float aqtweaks$fullFontCost(float amount, ItemStack stack, EntityPlayer player, float ignored,
            boolean crafting, boolean sim) {
        return ThaumcraftPerkHooks.fullFontCost(player, amount, crafting);
    }

    @Inject(method = "consumeVis", at = @At("RETURN"))
    private void aqtweaks$fullFontStamp(ItemStack stack, EntityPlayer player, float amount, boolean crafting,
            boolean sim, CallbackInfoReturnable<Boolean> cir) {
        if (sim || crafting) return;
        if (cir.getReturnValue() == null || !cir.getReturnValue()) return;
        ThaumcraftPerkHooks.fullFontStamp(player, crafting);
    }
}
