package com.apocollis.aqtweaks.thaumcraft;

import com.apocollis.aqtweaks.ArcanaQuestTweaksConfig;

import net.minecraft.util.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Loader;
import thaumcraft.api.ThaumcraftApi;

/** Mana-Weave wiring. Does nothing unless both Thaumcraft and Botania are loaded. */
public final class ManaRepairModule {
    private ManaRepairModule() {}

    public static boolean active() {
        return Loader.isModLoaded("thaumcraft") && Loader.isModLoaded("botania");
    }

    /** Call from init: after both mods have registered their items and before Thaumcraft's research loads in postInit. */
    public static void init() {
        if (!active() || !ArcanaQuestTweaksConfig.ThaumcraftConfig.enableManaRepair) return;
        MinecraftForge.EVENT_BUS.register(new ManaRepairHandler());
        ThaumcraftApi.registerResearchLocation(new ResourceLocation("aqtweaks", "research/mana_infusion"));
        ThaumcraftApi.addInfusionCraftingRecipe(new ResourceLocation("aqtweaks", "mana_repair"), new InfusionManaRepairRecipe());
    }
}
