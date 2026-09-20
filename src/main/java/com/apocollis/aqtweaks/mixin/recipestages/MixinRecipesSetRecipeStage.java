package com.apocollis.aqtweaks.mixin.recipestages;

import com.apocollis.aqtweaks.gamestages.GameStagesRecipeIndex;
import com.blamejared.recipestages.handlers.Recipes;
import crafttweaker.api.item.IIngredient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Recipes.class, remap = false)
public abstract class MixinRecipesSetRecipeStage {

    @Inject(method = "setRecipeStage(Ljava/lang/String;Lcrafttweaker/api/item/IIngredient;)V", at = @At("HEAD"))
    private static void aqtweaks$captureOutputStage(String stage, IIngredient ingredient, CallbackInfo ci) {
        GameStagesRecipeIndex.add(stage, ingredient);
    }
}
