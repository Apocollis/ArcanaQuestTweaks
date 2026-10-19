package com.apocollis.aqtweaks.mixin.thaumcraft;

import com.apocollis.aqtweaks.thaumcraft.InfernalFurnaceHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.FurnaceRecipes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import thaumcraft.common.tiles.devices.TileInfernalFurnace;

@Mixin(value = TileInfernalFurnace.class, remap = false)
public abstract class MixinTileInfernalFurnace {

    @Redirect(
            method = {"canSmelt", "func_73660_a"},
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/crafting/FurnaceRecipes;getSmeltingResult(Lnet/minecraft/item/ItemStack;)Lnet/minecraft/item/ItemStack;",
                    remap = true
            )
    )
    private ItemStack aqtweaks$getSmeltingResult(FurnaceRecipes recipes, ItemStack stack) {
        return InfernalFurnaceHelper.getSmeltingResult(recipes, stack);
    }

    @Inject(method = "getSmeltingBonus", at = @At("RETURN"), cancellable = true)
    private void aqtweaks$getSmeltingBonus(ItemStack in, CallbackInfoReturnable<ItemStack[]> cir) {
        TileInfernalFurnace self = (TileInfernalFurnace) (Object) this;
        ItemStack[] updated = InfernalFurnaceHelper.appendGatedSmeltingBonus(in, self.getWorld(), cir.getReturnValue());
        if (updated != cir.getReturnValue()) {
            cir.setReturnValue(updated);
        }
    }

    @Redirect(
            method = "ejectItem",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/item/crafting/FurnaceRecipes;getSmeltingExperience(Lnet/minecraft/item/ItemStack;)F",
                    remap = true
            )
    )
    private float aqtweaks$getSmeltingExperience(FurnaceRecipes recipes, ItemStack stack) {
        return InfernalFurnaceHelper.getSmeltingExperience(recipes, stack);
    }
}
