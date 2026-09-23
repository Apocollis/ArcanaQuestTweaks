package com.apocollis.aqtweaks.reskillable;

import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public class StitchRecipeEvents {

    @SubscribeEvent
    public void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        event.getRegistry().register(new RecipeStitchPoppet().setRegistryName("aqtweaks", "stitch_poppet"));
    }
}
