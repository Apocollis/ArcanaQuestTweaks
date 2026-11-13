package com.apocollis.aqtweaks.weapon;

import com.apocollis.aqtweaks.ArcanaQuestTweaks;
import net.minecraft.item.Item;
import net.minecraft.item.crafting.IRecipe;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/** Registers the Infinity Blade and its optional placeholder recipe. */
@Mod.EventBusSubscriber(modid = ArcanaQuestTweaks.MODID)
public final class WeaponModule {

    public static final ItemInfinityBlade INFINITY_BLADE = new ItemInfinityBlade();

    private WeaponModule() {}

    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        event.getRegistry().register(INFINITY_BLADE);
    }

    @SubscribeEvent
    public static void registerRecipes(RegistryEvent.Register<IRecipe> event) {
        WeaponRecipeHandler.register(event);
    }
}
