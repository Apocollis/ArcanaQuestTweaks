package com.apocollis.aqtweaks.mixin.solcarrot;

import com.cazsius.solcarrot.client.FoodItemStacks;
import mezz.jei.Internal;
import mezz.jei.api.IJeiHelpers;
import mezz.jei.api.ingredients.IIngredientBlacklist;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = FoodItemStacks.class, remap = false)
public abstract class MixinFoodItemStacks {

    @Inject(method = "getAllFoods", at = @At("RETURN"))
    private static void aqtweaks$filterBlacklistedFoods(CallbackInfoReturnable<List<ItemStack>> cir) {
        List<ItemStack> list = cir.getReturnValue();
        if (list == null || list.isEmpty()) return;
        try {
            IJeiHelpers helpers = Internal.getHelpers();
            if (helpers == null) return;
            IIngredientBlacklist blacklist = helpers.getIngredientBlacklist();
            if (blacklist == null) return;
            list.removeIf(blacklist::isIngredientBlacklisted);
        } catch (Throwable ignored) {
        }
    }
}
