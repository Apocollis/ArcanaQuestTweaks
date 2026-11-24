package com.apocollis.aqtweaks.additions;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.oredict.ShapedOreRecipe;

@Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
public final class AdditionsRecipeHandler {
    private AdditionsRecipeHandler() {
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        if (!ArcanaQuestTweaksConfig.AdditionsModuleConfig.infinityBlade.enableDefaultRecipe) {
            return;
        }
        if (AdditionsModule.INFINITY_BLADE == null) {
            return;
        }
        ResourceLocation name = new ResourceLocation(ArcanaQuestTweaks.MODID, AdditionsModule.INFINITY_BLADE_ID);
        ShapedOreRecipe recipe = new ShapedOreRecipe(
                name,
                new ItemStack(AdditionsModule.INFINITY_BLADE),
                "NNN",
                "NSN",
                "NNN",
                'N', Items.NETHER_STAR,
                'S', Items.DIAMOND_SWORD
        );
        recipe.setRegistryName(name);
        event.getRegistry().register(recipe);
    }
}
