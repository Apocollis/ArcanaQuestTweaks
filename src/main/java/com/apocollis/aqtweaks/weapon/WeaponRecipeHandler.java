package com.apocollis.aqtweaks.weapon;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig.InfinityBladeModuleConfig;

import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapedRecipes;
import net.minecraft.util.NonNullList;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;

/**
 * The placeholder recipe: eight Nether Stars around a Diamond Sword. Registered only when Enable Default Recipe is
 * true (default false, the crafting method is still undecided).
 */
public final class WeaponRecipeHandler {
    private WeaponRecipeHandler() {}

    public static void register(RegistryEvent.Register<IRecipe> event) {
        if (!InfinityBladeModuleConfig.general.enableDefaultRecipe) return;
        Ingredient star = Ingredient.fromItem(Items.NETHER_STAR);
        Ingredient sword = Ingredient.fromItem(Items.DIAMOND_SWORD);
        NonNullList<Ingredient> grid = NonNullList.from(Ingredient.EMPTY, star, star, star, star, sword, star, star, star, star);
        ResourceLocation id = new ResourceLocation(ArcanaQuestTweaks.MODID, "infinity_blade");
        event.getRegistry().register(new ShapedRecipes(id.toString(), 3, 3, grid, new ItemStack(WeaponModule.INFINITY_BLADE)).setRegistryName(id));
    }
}
